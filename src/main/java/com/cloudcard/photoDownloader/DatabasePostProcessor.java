package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@ConditionalOnProperty(value = "downloader.postProcessor", havingValue = "DatabasePostProcessor")
public class DatabasePostProcessor implements PostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DatabasePostProcessor.class);

    @Value("${DatabasePostProcessor.override.photoFilePath:}")
    private String filePathOverride;

    @Value("${DatabasePostProcessor.query:}")
    private String updateQuery;

    @Value("${DatabasePostProcessor.query.paramNames:}")
    private String[] paramNames;

    @Value("${DatabasePostProcessor.query.paramTypes:}")
    private String[] paramTypes;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {

        if (!updateQuery.isEmpty() && paramNames.length != paramTypes.length) {
            log.error("The following settings must be comma separated lists with the same length as each other: " +
                "\n - downloader.metadata.db.update.params \n - downloader.metadata.db.update.paramTypes");
            System.exit(1);
        }

        log.info("         Update Query : " + updateQuery);
        log.info("          Param Names : " + String.join(" , ", paramNames));
        log.info("          Param Types : " + String.join(" , ", paramTypes));
        log.info("   File Path Override : " + filePathOverride);
    }

    @Override
    public PhotoFile process(Photo photo, String photoDirectory, PhotoFile photoFile) {

        try {
            updateDatabase(photo, photoFile);
            return photoFile;
        } catch (Exception e) {
            log.error("Post processing failed: " + e.getMessage() + "\nPrint stacktrace follows...");
            e.printStackTrace();
            return null;
        }
    }

    private void updateDatabase(Photo photo, PhotoFile file) throws Exception {

        if (file != null && !updateQuery.isEmpty()) {

            Object[] params = resolveParams(photo, file);
            int[] paramTypes = resolveParamTypes();
            jdbcTemplate.update(updateQuery, params, paramTypes);
        }
    }

    private Object[] resolveParams(Photo photo, PhotoFile file) {

        Map<String, Object> paramMap = createParamMap(photo, file);

        StringBuilder msg = new StringBuilder("updating database with values [");

        Object[] params = new Object[ paramNames.length ];
        for (int i = 0; i < paramNames.length; i++) {
            params[ i ] = paramMap.get(paramNames[ i ]);
            buildLogMessage(msg, params, i);
        }
        msg.append("]");
        log.info(msg.toString());

        return params;
    }

    private void buildLogMessage(StringBuilder msg, Object[] params, int i) {

        if (i > 0) msg.append(", ");
        msg.append(paramNames[ i ] + ":" + params[ i ].toString());
    }

    private Map<String, Object> createParamMap(Photo photo, PhotoFile file) {

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> paramMap = objectMapper.convertValue(photo, Map.class);
        Map<String, Object> personMap = objectMapper.convertValue(photo.getPerson(), Map.class);
        paramMap.putAll(personMap);
        paramMap.put("timestamp", Timestamp.valueOf(LocalDateTime.now().withSecond(0).withNano(0)));
        paramMap.put("fileName", filePathOverride.equals("") ? file.getFileName() : filePathOverride + file.getBaseName() + ".jpg");
        return paramMap;
    }

    private int[] resolveParamTypes() throws Exception {

        int[] bacon = new int[ paramTypes.length ];
        for (int i = 0; i < paramTypes.length; i++) {
            bacon[ i ] = resolveParamType(paramTypes[ i ]);
        }
        return bacon;
    }

    private int resolveParamType(String typeName) throws Exception {

        return Types.class.getField(typeName).getInt(null);
    }

}
