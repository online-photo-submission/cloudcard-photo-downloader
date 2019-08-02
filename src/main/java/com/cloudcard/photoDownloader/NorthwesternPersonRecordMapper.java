package com.cloudcard.photoDownloader;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class NorthwesternPersonRecordMapper implements RowMapper<NorthwesternPersonRecord> {

    @Override
    public NorthwesternPersonRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        NorthwesternPersonRecord record = new NorthwesternPersonRecord();

        record.setFirstName(rs.getString("firstname"));
        record.setLastName(rs.getString("lastname"));
        record.setIdentifier(rs.getString("ssnnumber"));
        record.setPhotoUpdated(rs.getTimestamp("photoUpdated"));
        record.setExpirationDate(rs.getTimestamp("expirationDate"));

        doesRecordNeedUpdated(record);

        return record;
    }

    private void doesRecordNeedUpdated(NorthwesternPersonRecord record) {
        if (record.getExpirationDate() != null && Math.abs(ChronoUnit.DAYS.between(record.getExpirationDate().toLocalDateTime(), LocalDateTime.now().plusDays(60))) < 60.0) {
            record.setCardPhoto(false);
        }
    }
}
