package mock.genetec

import groovy.transform.CompileStatic
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.filter.OncePerRequestFilter

import java.util.concurrent.ConcurrentHashMap

@SpringBootApplication
class App {
  static void main(String[] args) {
    SpringApplication.run(App, args)
  }

  @Bean
  InMemoryStore inMemoryStore() {
    // Two accounts hard-coded by default (alias in URL -> accountId in system)
    def store = new InMemoryStore()
    store.accounts.put("alias", new Account(accountId: "acct_001"))   // per spec uses 'alias'
    store.accounts.put("alias2", new Account(accountId: "acct_002"))  // second hard-coded account
    return store
  }
}

/** ===== Models / DTOs ===== **/

@CompileStatic
class Account {
  String accountId
}

@CompileStatic
class SystemData {
  String externalId
}

@CompileStatic
class Identity {
  String accountId
  String identityId
  SystemData systemData
}

@CompileStatic
class IdentitiesListResponse {
  List<Identity> identities
  int totalItems
  String continuation
}

/** Requests */
@Validated
@CompileStatic
class IdentityCreateRequest {
  // identityId optional on create; if missing, server generates a UUID
  String identityId

  @Valid
  SystemDataRequest systemData

  @CompileStatic
  static class SystemDataRequest {
    @NotBlank
    String externalId
  }
}

@Validated
@CompileStatic
class IdentityUpdateRequest {
  @Valid
  IdentityCreateRequest.SystemDataRequest systemData
}

/** ===== In-memory store ===== **/

@Component
@CompileStatic
class InMemoryStore {
  // accountAlias -> Account
  final Map<String, Account> accounts = new ConcurrentHashMap<>()

  // accountAlias -> (identityId -> Identity)
  final Map<String, Map<String, Identity>> identitiesByAlias = new ConcurrentHashMap<>()

  Map<String, Identity> identitiesForAlias(String alias) {
    identitiesByAlias.computeIfAbsent(alias) { new ConcurrentHashMap<String, Identity>() }
  }
}

/** ===== Auth Filter ===== **/

@Component
class BearerAuthFilter extends OncePerRequestFilter {
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Only protect /api/v4/**
    return !request.requestURI?.startsWith("/api/v4/")
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
    def auth = request.getHeader("Authorization")
    if (auth == null || !auth.startsWith("Bearer ")) {
      response.status = HttpStatus.UNAUTHORIZED.value()
      response.contentType = "application/json"
      response.writer.write('{"error":"missing_or_invalid_authorization","message":"Authorization: Bearer <token> header is required."}')
      return
    }
    filterChain.doFilter(request, response)
  }
}

/** ===== Controllers ===== **/

@RestController
@RequestMapping("/api/v4/accounts/{alias}")
@CompileStatic
class AccountsController {

  private final InMemoryStore store

  AccountsController(InMemoryStore store) {
    this.store = store
  }

  // /api/v4/accounts/alias/config
  @GetMapping("/config")
  ResponseEntity<?> config(@PathVariable("alias") String alias) {
    def acct = store.accounts.get(alias)
    if (acct == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body([error: "account_not_found", alias: alias])
    }
    return ResponseEntity.ok([accountId: acct.accountId])
  }
}

@RestController
@RequestMapping("/api/v4/accounts/{alias}/identities")
@CompileStatic
class IdentitiesController {

  private final InMemoryStore store

  IdentitiesController(InMemoryStore store) {
    this.store = store
  }

  private Account requireAccount(String alias) {
    def acct = store.accounts.get(alias)
    if (acct == null) throw new AccountNotFound(alias)
    return acct
  }

  // GET /api/v4/accounts/{alias}/identities?externalId=asdf
  @GetMapping
  IdentitiesListResponse list(
      @PathVariable("alias") String alias,
      @RequestParam(name = "ExternalId", required = false) String externalId
  ) {
    def acct = requireAccount(alias)
    def all = store.identitiesForAlias(alias).values().toList()

    def filtered = (externalId != null && externalId.trim().length() > 0)
        ? all.findAll { it.systemData?.externalId == externalId }
        : all

    return new IdentitiesListResponse(
        identities: filtered,
        totalItems: filtered.size(),
        continuation: null
    )
  }

  // POST /api/v4/accounts/{alias}/identities
  @PostMapping
  ResponseEntity<Identity> create(
      @PathVariable("alias") String alias,
      @Valid @RequestBody IdentityCreateRequest req
  ) {
    def acct = requireAccount(alias)
    def idMap = store.identitiesForAlias(alias)

    def newId = (req.identityId?.trim()) ? req.identityId.trim() : UUID.randomUUID().toString()
    if (idMap.containsKey(newId)) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(null)
    }

    def identity = new Identity(
        accountId: acct.accountId,
        identityId: newId,
        systemData: new SystemData(externalId: req.systemData?.externalId)
    )

    idMap.put(newId, identity)
    return ResponseEntity.status(HttpStatus.CREATED).body(identity)
  }

  // GET /api/v4/accounts/{alias}/identities/{identityId}
  @GetMapping("/{identityId}")
  Identity getOne(@PathVariable("alias") String alias, @PathVariable("identityId") String identityId) {
    requireAccount(alias)
    def idMap = store.identitiesForAlias(alias)
    def found = idMap.get(identityId)
    if (found == null) throw new IdentityNotFound(alias, identityId)
    return found
  }

  // PUT /api/v4/accounts/{alias}/identities/{identityId}
  @PutMapping("/{identityId}")
  Identity update(
      @PathVariable("alias") String alias,
      @PathVariable("identityId") String identityId,
      @Valid @RequestBody IdentityUpdateRequest req
  ) {
    def acct = requireAccount(alias)
    def idMap = store.identitiesForAlias(alias)

    def existing = idMap.get(identityId)
    if (existing == null) throw new IdentityNotFound(alias, identityId)

    if (req.systemData?.externalId != null) {
      existing.systemData = existing.systemData ?: new SystemData()
      existing.systemData.externalId = req.systemData.externalId
    }

    // ensure accountId always matches the account
    existing.accountId = acct.accountId
    idMap.put(identityId, existing)
    return existing
  }

  // DELETE /api/v4/accounts/{alias}/identities/{identityId}
  @DeleteMapping("/{identityId}")
  ResponseEntity<?> delete(@PathVariable("alias") String alias, @PathVariable("identityId") String identityId) {
    requireAccount(alias)
    def idMap = store.identitiesForAlias(alias)
    def removed = idMap.remove(identityId)
    if (removed == null) throw new IdentityNotFound(alias, identityId)
    return ResponseEntity.noContent().build()
  }
}

/** ===== Errors / Exception Mapping ===== **/

@CompileStatic
class AccountNotFound extends RuntimeException {
  final String alias
  AccountNotFound(String alias) {
    super("Account alias not found: ${alias}")
    this.alias = alias
  }
}

@CompileStatic
class IdentityNotFound extends RuntimeException {
  final String alias
  final String identityId
  IdentityNotFound(String alias, String identityId) {
    super("Identity not found: ${alias}/${identityId}")
    this.alias = alias
    this.identityId = identityId
  }
}

@RestControllerAdvice
@CompileStatic
class ApiExceptionHandler {

  @ExceptionHandler(AccountNotFound)
  ResponseEntity<?> handleAccountNotFound(AccountNotFound ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body([error: "account_not_found", alias: ex.alias])
  }

  @ExceptionHandler(IdentityNotFound)
  ResponseEntity<?> handleIdentityNotFound(IdentityNotFound ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body([error: "identity_not_found", alias: ex.alias, identityId: ex.identityId])
  }

  @ExceptionHandler(Exception)
  ResponseEntity<?> handleGeneric(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body([error: "internal_error", message: ex.message])
  }
}