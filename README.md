# Minimal Geocoder
Minimal Geocoder is a simple Geocoding API based on data from Openstreetmap and a REST interface using Spring Framework.

Background information (in German): http://blog.plechinger.at/einfaches-geocoding-mit-open-street-map/.

## Example:

**Request**:

```
http://localhost:8080/geocode?q=hauptplatz+graz
```

**Response**:

```json
[
  {
    "street" : "Hauptplatz",
    "housenumber" : "9",
    "postcode" : "8010",
    "city" : "Graz",
    "country" : "AT",
    "longitude" : 15.437595916666666,
    "latitude" : 47.071199916666664
  },
  ...
]
```

## Prerequisites
 - Computer with Java >1.7
 - Postgres Server with installed PostGis extension
 
Installation guide for PostGis: [http://postgis.net/install]()

## Installation

At first you have to create a new database and activate the extensions `hstore` and `postgis`

```sql
CREATE EXTENSION postgis;  
CREATE EXTENSION hstore;  
```

After that you need [Osmosis](http://wiki.openstreetmap.org/wiki/Osmosis) and a dataset from Openstreetmap from [here](http://wiki.openstreetmap.org/wiki/Downloading_data)
or [here](http://download.geofabrik.de/).

Create the database schema from Osmosis:

```bash
psql -d <database> -f <osmosis-folder>/script/pgsnapshot_schema_0.6.sql
```

Import the data:

```bash
osmosis --read-xml file="datafile.osm" --write-apidb host="localhost" database="<dbname>" user="<dbuser>" password="<dbpassword>"  
```

With that configuration (and additional indices for the hstore columns), a single geocoding operation takes an average of 760ms, reverse geocoding up to 2600ms,
thats far to long. To speed things up, a optimized table should be created and this results in about 20 ms for geocoding and 120ms for reverse geocoding.

Performance improvements:
 - precalculated Point for reverse geocoding
 - preindexed full text search
 - minimize calls of functions and subqueries
 - no hstore queries

**Optimization table**: (you can also find the script in `optimize.sql`)
```sql
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
```

## Run server

Change the database credentials in `src/main/resources/application.properties`.

Run Maven `spring-boot:run`
```bash
mvn spring-boot:run
```

## Geocode

To Geocode an address, just call
```
http://localhost:8080/geocode{?q}
```

To get all addresses, call. **Be careful as this query can retrieve thousands of rows.**
```
http://localhost:8080/geocode`all{?q,sort}
```

You can also sort the result by adding the `sort` parameter:
```
#Sort by city ASC
http://localhost:8080/geocode?q=<address>&sort=city
#or
http://localhost:8080/geocode?q=<address>&sort=city,ASC

#Sort by city, DESC
http://localhost:8080/geocode?q=<address>&sort=city,DESC

#Sort by city, DESC and street ASC
http://localhost:8080/geocode?q=<address>&sort=city,DESC&sort=street,ASC
```

### Pagination and limiting number of results.

To paginate over the results, there are two possibilities:

**Slicing**: Separating the Result with no knowledge about the total number of elements.

Parameters:
 - `q`: Address to geocode
 - `sort`: Sorting, see example above
 - `page`: Page number, starting at `0`, default `0`
 - `size`: Number of elements per page, default `10`
 
```
http://localhost:8080/geocode/sliced{?q,sort,page,size}
```

**Paging**: Separating the Result with knowledge about the total number of elements by making a `COUNT(*)` 
statement before executing the query. Depending on the number of total elements and computer resources
this can take longer than the other queries.

Parameters:
 - `q`: Address to geocode
 - `sort`: Sorting, see example above
 - `page`: Page number, starting at `0`, default `0`
 - `size`: Number of elements per page, default `10`
 
```
http://localhost:8080/geocode/paged{?q,sort,page,size}
```

## Reverse geocoding

Slicing and Pagination works exactly the same like with geocoding.

**Reverse Geocode**:

```
#Gives the address with the shortest distance to the given coordinates
http://localhost:8080/reverse{?lat,lng}

#Returns All the addresses for the given coordinates:
http://localhost:8080/reverse/all{?lat,lng,sort} #be careful, no filtering beside lat and lng.
http://localhost:8080/reverse/sliced{?lat,lng,sort,page,size}
http://localhost:8080/reverse/paged{?lat,lng,sort,page,size}
```

**Request**:

```
http://localhost:8080/reverse?lat=47.07119&lon=15.437595916
```

**Result**:

```json
{
    "street" : "Hauptplatz",
    "housenumber" : "9",
    "postcode" : "8010",
    "city" : "Graz",
    "country" : "AT",
    "longitude" : 15.43759591666666,
    "latitude" : 47.071199916666664,
    "distance" : 1.102684541
}
```
