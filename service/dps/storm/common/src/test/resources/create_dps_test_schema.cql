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
    processed_files_count int,
    state varchar,
    info varchar,
    start_time timestamp,
    finish_time timestamp,
    sent_time timestamp,
    errors int,
    PRIMARY KEY(task_id)
);


CREATE TABLE notifications (
    resource_num int,
    task_id bigint,
    topology_name varchar,
    resource varchar,
    state varchar,
    info_text text,
    additional_informations text,
    result_resource varchar,
    PRIMARY KEY(task_id,resource_num)
) WITH CLUSTERING ORDER BY (resource_num DESC);


CREATE TABLE error_notifications (
    task_id bigint,
    error_type timeuuid,
    error_message text,
    resource varchar,
    additional_informations text,
    PRIMARY KEY((task_id,error_type),resource)
);

CREATE TABLE error_counters (
    task_id bigint,
    error_type timeuuid,
    error_count counter,
    PRIMARY KEY(task_id,error_type)
);

CREATE TABLE general_statistics (
    task_id bigint,
    parent_xpath varchar,
    node_xpath varchar,
    occurrence counter,
    PRIMARY KEY(task_id, parent_xpath, node_xpath)
);

CREATE TABLE node_statistics (
    task_id bigint,
    node_xpath varchar,
    node_value varchar,
    occurrence counter,
    PRIMARY KEY((task_id, node_xpath), node_value)
);

CREATE TABLE attribute_statistics (
    task_id bigint,
    node_xpath varchar,
    node_value varchar,
    attribute_name varchar,
    attribute_value varchar,
    occurrence counter,
    PRIMARY KEY((task_id, node_xpath, node_value), attribute_name, attribute_value)
);

CREATE TABLE statistics_reports (
task_id bigint,
report_data blob,
PRIMARY KEY(task_id)
);
