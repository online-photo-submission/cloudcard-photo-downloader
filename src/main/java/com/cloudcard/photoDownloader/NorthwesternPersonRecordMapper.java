package com.cloudcard.photoDownloader;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class NorthwesternPersonRecordMapper implements RowMapper<NorthwesternPersonRecord> {

    @Override
    public NorthwesternPersonRecord mapRow(ResultSet rs, int rowNum) throws SQLException {

        NorthwesternPersonRecord record = new NorthwesternPersonRecord();

        record.setFirstName(rs.getString("firstname"));
        record.setLastName(rs.getString("lastname"));
        record.setIdNumber(rs.getString("IDNumber"));
        record.setPrefixedIdNumber(rs.getString("ssnnumber"));
        record.setPhotoUpdated(rs.getTimestamp("photoUpdated"));
        record.setExpirationDate(rs.getTimestamp("expirationDate"));

        return record;
    }
}
