# Minimal Geocoder
Minimal Geocoder is a simple Geocoding API based on data from Openstreetmap and a REST interface using Spring Framework.

##Example:

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

##Prerequisites
 - Computer with Java >1.7
 - Postgres Server with installed PostGis extension
 
Installation guide for PostGis: [http://postgis.net/install]()

##Installation

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

To have a better performance you can create indicies for the `hstore` column

```sql
CREATE INDEX idx_nodes_tags ON nodes USING gist(tags);  
CREATE INDEX idx_relations_tags ON relations USING gist(tags);  
CREATE INDEX idx_ways_tags ON ways USING gist(tags);  
```

##Run server

Change the database credentials in `src/main/resources/application.properties`.

Run Maven `spring-boot:run`

```bash
mvn spring-boot:run
```

##Geocode

To Geocode an address, just call

````
http://localhost:8080/geocode{?q,sort}
```

You can also sort the result by adding the `sort` parameter:

````
#Sort by city ASC
http://localhost:8080/geocode?q=<address>&sort=city
#or
http://localhost:8080/geocode?q=<address>&sort=city,ASC

#Sort by city, DESC
http://localhost:8080/geocode?q=<address>&sort=city,DESC

#Sort by city, DESC and street ASC
http://localhost:8080/geocode?q=<address>&sort=city,DESC&sort=street,ASC
```
###Pagination and limiting number of results.

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

