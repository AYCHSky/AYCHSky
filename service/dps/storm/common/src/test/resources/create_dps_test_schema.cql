/*

   ! IMPORTANT NOTICE !
   Due to a bug in parsing by cassandraunit, statements must end with a semicolon directly followed by
   a newline character - particularly, there cannot be whitespace before line end

   This file assumes that it is executed in a particular keyspace context.
   In order to create a keyspace, you can use e.g.

   CREATE KEYSPACE ecloud_test
           WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};
   USE ecloud_test;
*/

USE ecloud_test;
CREATE TABLE basic_info (
    task_id bigint,
    topology_name varchar,
    expected_size int,
    PRIMARY KEY(task_id, topology_name)
);

CREATE TABLE notifications (
    task_id bigint,
    topology_name varchar,
    resource varchar,
    state varchar,
    info_text text,
    additional_informations text,
    result_resource varchar,
    PRIMARY KEY(task_id, topology_name, resource)
);