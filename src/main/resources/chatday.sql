create database chatday;
use chatday;

create table msg(
	id int primary key auto_increment,
	username varchar(20),
	sendtime bigint, --long 类型
	info varchar(512)
);