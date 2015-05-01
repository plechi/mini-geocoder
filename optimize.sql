CREATE TABLE geocode_optimized
AS SELECT
     w.tags -> 'addr:street'                            AS street,
     w.tags -> 'addr:housenumber'                       AS housenumber,
     w.tags -> 'addr:postcode'                          AS postcode,
     w.tags -> 'addr:city'                              AS city,
     w.tags -> 'addr:country'                           AS country,
     AVG(ST_X(n.geom))                                  AS longitude,
     AVG(ST_Y(n.geom))                                  AS latitude,
     to_tsvector(concat_ws(' ', w.tags -> 'addr:street',
                           w.tags -> 'addr:housenumber',
                           w.tags -> 'addr:postcode',
                           w.tags -> 'addr:city',
                           w.tags -> 'addr:country'
                 ))                                     AS full_text,
     st_makepoint(AVG(ST_X(n.geom)), AVG(ST_Y(n.geom))) AS point
   FROM ways w
     INNER JOIN way_nodes wn ON w.id = wn.way_id
     INNER JOIN nodes n ON n.id = wn.node_id
   WHERE exist(w.tags, 'addr:housenumber') AND exist(w.tags, 'addr:street')
   GROUP BY housenumber, street, postcode, city, country;

CREATE INDEX idx_geocode_full_text ON geocode_optimized USING GIN (full_text);