package com.cloudcard.photoDownloader;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactCustomerMapper implements RowMapper<TransactCustomer> {
    @Override
    public TransactCustomer mapRow(ResultSet rs, int rowNum) throws SQLException {
        TransactCustomer customer = new TransactCustomer();

//        TODO: Fill in record setters here

        return customer;
    }
}
