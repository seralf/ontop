CREATE TABLE "GEOMS" (id INT PRIMARY KEY , the_geom geometry, name TEXT);
INSERT INTO "GEOMS" VALUES (1, 'POLYGON((2 2, 7 2, 7 5, 2 5, 2 2))', 'small rectangle');
INSERT INTO "GEOMS" VALUES (2, 'POLYGON((1 1, 8 1, 8 7, 1 7, 1 1))', 'large rectangle');
INSERT INTO "GEOMS" VALUES (3, 'POINT(2.2945 48.8584)', 'Eiffel Tower');
INSERT INTO "GEOMS" VALUES (4, 'POINT(-0.0754 51.5055)', 'Tower Bridge');
INSERT INTO "GEOMS" VALUES (5, 'POLYGON((3 3, 8 3, 8 6, 3 6, 3 3))', 'small rectangle 2');
INSERT INTO "GEOMS" VALUES (6, 'POLYGON((1 1, 8 1, 8 7, 1 7, 1 1))', 'large rectangle 2');
INSERT INTO "GEOMS" VALUES (7, 'POLYGON((1 1, 2 1, 2 2, 1 2, 1 1))', 'very small rectangle');
INSERT INTO "GEOMS" VALUES (8, 'POLYGON((1 2, 2 2, 2 3, 1 3, 1 2))', 'small rectangle 3');
INSERT INTO "GEOMS" VALUES (9, 'LINESTRING(1 2, 2 2, 3 2)', 'short horizontal line');
INSERT INTO "GEOMS" VALUES (10, 'LINESTRING(1 2, 10 2)', 'long horizontal line');
INSERT INTO "GEOMS" VALUES (11, 'POINT(2 2)', 'point');
INSERT INTO "GEOMS" VALUES (12, 'LINESTRING(2 1, 2 10)', 'long vertical line');

