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

package at.plechinger.minigeocode.repository;

import at.plechinger.minigeocode.data.GeocodeResult;
import at.plechinger.minigeocode.data.ReverseGeocodeResult;
import at.plechinger.minigeocode.mapper.GeocodeRowMapper;
import at.plechinger.minigeocode.mapper.ReverseGeocodeRowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

/**
 * Created by lukas on 01.05.15.
 */
@Repository
@Slf4j
public class GeocodeRepository extends JdbcDaoSupport {

    @Autowired
    public void setDatasource(DataSource datasource) {
        super.setDataSource(datasource);
    }

    @Autowired
    private GeocodeRowMapper geocodeRowMapper;

    @Autowired
    private ReverseGeocodeRowMapper reverseGeocodeRowMapper;

    private static final String GEOCODE_SQL = "SELECT\n" +
            "  street,\n" +
            "  housenumber,\n" +
            "  postcode,\n" +
            "  city,\n" +
            "  country,\n" +
            "  longitude,\n" +
            "  latitude\n" +
            "FROM (SELECT\n" +
            "        w.tags -> 'addr:street'      AS street,\n" +
            "        w.tags -> 'addr:housenumber' AS housenumber,\n" +
            "        w.tags -> 'addr:postcode'    AS postcode,\n" +
            "        w.tags -> 'addr:city'        AS city,\n" +
            "        w.tags -> 'addr:country'     AS country,\n" +
            "        AVG(ST_X(n.geom))            AS longitude,\n" +
            "        AVG(ST_Y(n.geom))            AS latitude,\n" +
            "        concat_ws(' ', w.tags -> 'addr:street',\n" +
            "                  w.tags -> 'addr:housenumber',\n" +
            "                  w.tags -> 'addr:postcode',\n" +
            "                  w.tags -> 'addr:city',\n" +
            "                  w.tags -> 'addr:country'\n" +
            "        )                            AS full_text\n" +
            "      FROM ways w\n" +
            "        INNER JOIN way_nodes wn ON w.id = wn.way_id\n" +
            "        INNER JOIN nodes n ON n.id = wn.node_id\n" +
            "      WHERE exist(w.tags, 'addr:housenumber') AND exist(w.tags, 'addr:street')\n" +
            "      GROUP BY housenumber, street, postcode, city, country\n" +
            "     ) geocode\n" +
            "WHERE to_tsvector(full_text) @@ plainto_tsquery(?) ";

    private static final String REVERSE_GEOCODE_SQL = "SELECT\n" +
            "  street,\n" +
            "  housenumber,\n" +
            "  postcode,\n" +
            "  city,\n" +
            "  country,\n" +
            "  longitude,\n" +
            "  latitude,\n" +
            "  CAST (st_distance_sphere(st_makepoint(longitude,latitude), st_makepoint(?,?)) AS FLOAT) as distance\n" +
            "FROM (SELECT\n" +
            "        w.tags -> 'addr:street'      AS street,\n" +
            "        w.tags -> 'addr:housenumber' AS housenumber,\n" +
            "        w.tags -> 'addr:postcode'    AS postcode,\n" +
            "        w.tags -> 'addr:city'        AS city,\n" +
            "        w.tags -> 'addr:country'     AS country,\n" +
            "        AVG(ST_X(n.geom))            AS longitude,\n" +
            "        AVG(ST_Y(n.geom))            AS latitude\n" +
            "      FROM ways w\n" +
            "        INNER JOIN way_nodes wn ON w.id = wn.way_id\n" +
            "        INNER JOIN nodes n ON n.id = wn.node_id\n" +
            "      WHERE exist(w.tags, 'addr:housenumber') AND exist(w.tags, 'addr:street')\n" +
            "      GROUP BY housenumber, street, postcode, city, country\n" +
            "     ) geocode ";


    private static final String COUNT_SQL = "SELECT COUNT(*) AS count FROM (%s) counter";

    //-----GEOCODING-----
    public List<GeocodeResult> findAll(String query, Sort sort) {
        String sql = GEOCODE_SQL + sortString(sort);
        return getJdbcTemplate().query(sql, new Object[]{query}, geocodeRowMapper);
    }

    public Slice<GeocodeResult> findSlice(String query, Pageable pageable) {
        String sql = GEOCODE_SQL + pageableString(pageable);
        List<GeocodeResult> result = getJdbcTemplate().query(sql, new Object[]{query}, geocodeRowMapper);
        boolean last = result.size() < pageable.getPageSize();
        return new SliceImpl<GeocodeResult>(result, pageable, !last);
    }

    public Page<GeocodeResult> findPage(String query, Pageable pageable) {
        String countSql = String.format(COUNT_SQL, GEOCODE_SQL);
        Long total = getJdbcTemplate().queryForObject(countSql, new Object[]{query}, Long.class);
        List<GeocodeResult> content = findSlice(query, pageable).getContent();
        return new PageImpl<GeocodeResult>(content, pageable, total);
    }

    //-----GEOCODING-----
    public List<ReverseGeocodeResult> findReverseAll(Double longitude, Double latitude, Sort sort) {
        String sql = REVERSE_GEOCODE_SQL + sortString(sort);
        return getJdbcTemplate().query(sql, new Object[]{longitude, latitude}, reverseGeocodeRowMapper);
    }

    public Slice<ReverseGeocodeResult> findReverseSlice(Double longitude, Double latitude, Pageable pageable) {
        String sql = REVERSE_GEOCODE_SQL + pageableString(pageable);
        List<ReverseGeocodeResult> result = getJdbcTemplate().query(sql, new Object[]{longitude, latitude}, reverseGeocodeRowMapper);
        boolean last = result.size() < pageable.getPageSize();
        return new SliceImpl<ReverseGeocodeResult>(result, pageable, !last);
    }

    public Page<ReverseGeocodeResult> findReversePage(Double longitude, Double latitude, Pageable pageable) {
        String countSql = String.format(COUNT_SQL, REVERSE_GEOCODE_SQL);

        Long total = getJdbcTemplate().queryForObject(countSql, new Object[]{longitude, latitude}, Long.class);
        List<ReverseGeocodeResult> content = findReverseSlice(longitude, latitude, pageable).getContent();
        return new PageImpl<ReverseGeocodeResult>(content, pageable, total);
    }

    private String sortString(Sort sort) {

        StringBuilder p = new StringBuilder();

        p.append(" ORDER BY ");

        boolean first = true;
        for (Sort.Order order : sort) {
            if (!first) {
                p.append(',');
            }
            p.append(' ').append(order.getProperty()).append(' ').append(order.getDirection());
            first = false;
        }

        return p.toString();
    }

    private String pageableString(Pageable pageable) {
        StringBuilder p = new StringBuilder();

        if (pageable.getSort() != null) {
            p.append(sortString(pageable.getSort()));
        }

        p.append(" LIMIT ").append(pageable.getPageSize()).append(" OFFSET ").append(pageable.getOffset());
        return p.toString();
    }
}
