create database chatshell default character set utf8 collate utf8_general_ci;
use chatshell;

create table msg(
	id int primary key auto_increment,
	username varchar(20),
	sendtime bigint, /*long 类型*/
	info varchar(512)
)engine = innodb default character set utf8 collate utf8_general_ci;
