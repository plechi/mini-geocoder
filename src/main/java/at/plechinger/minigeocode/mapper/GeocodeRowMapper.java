/*
 Copyright 2015 Lukas Plechinger

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package at.plechinger.minigeocode.mapper;

import at.plechinger.minigeocode.data.GeocodeResult;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a sql ResultSet to a GeocodeResult.
 */
@Component
public class GeocodeRowMapper implements RowMapper<GeocodeResult> {
    @Override
    public GeocodeResult mapRow(ResultSet row, int index) throws SQLException {
        GeocodeResult result = new GeocodeResult();
        result.setStreet(row.getString("street"));
        result.setHousenumber(row.getString("housenumber"));
        result.setPostcode(row.getString("postcode"));
        result.setCity(row.getString("city"));
        result.setCountry(row.getString("country"));

        result.setLongitude(row.getDouble("longitude"));
        result.setLatitude(row.getDouble("latitude"));

        return result;
    }
}
