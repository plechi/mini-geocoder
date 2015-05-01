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
import at.plechinger.minigeocode.data.ReverseGeocodeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a sql ResultSet to a GeocodeResult.
 */
@Component
public class ReverseGeocodeRowMapper implements RowMapper<ReverseGeocodeResult> {

    @Autowired
    private GeocodeRowMapper geocodeRowMapper;

    @Override
    public ReverseGeocodeResult mapRow(ResultSet row, int index) throws SQLException {
        ReverseGeocodeResult result = new ReverseGeocodeResult();
        GeocodeResult original = geocodeRowMapper.mapRow(row, index);

        result.setStreet(original.getStreet());
        result.setHousenumber(original.getHousenumber());
        result.setPostcode(original.getPostcode());
        result.setCity(original.getCity());
        result.setCountry(original.getCountry());

        result.setLongitude(original.getLongitude());
        result.setLatitude(original.getLatitude());

        result.setDistance(row.getDouble("distance"));

        return result;
    }
}
