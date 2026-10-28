/*==============================================================*/
/* DBMS name:      MySQL 5.0                                    */
/* Created on:     2025/3/31 10:23:27                           */
/*==============================================================*/


drop table if exists comments;

drop table if exists dormitories;

drop table if exists host;

drop table if exists orders;

drop table if exists rooms;

drop table if exists visitors;

/*==============================================================*/
/* User: User_1                                                 */
/*==============================================================*/
/*create user User_1;*/

/*==============================================================*/
/* Table: comments                                              */
/*==============================================================*/
create table comments
(
   comID                numeric(8,0) not null,
   dorName              varchar(255),
   class                numeric(8,0),
   content              text,
   time                 time,
   primary key (comID)
);

/*==============================================================*/
/* Table: dormitories                                           */
/*==============================================================*/
create table dormitories
(
   dorName              varchar(255) not null,
   address              text,
   class                numeric(8,0),
   roomNum              numeric(8,0),
   primary key (dorName)
);

/*==============================================================*/
/* Table: host                                                  */
/*==============================================================*/
create table host
(
   hostID               numeric(18,0) not null,
   dorName              varchar(255),
   name                 text,
   gender               text,
   Tel                  numeric(11,0),
   primary key (hostID)
);

/*==============================================================*/
/* Table: orders                                                */
/*==============================================================*/
create table orders
(
   ordID                numeric(8,0) not null,
   roomID               numeric(8,0),
   dorName              varchar(255),
   visID                numeric(18,0),
   remark               text,
   primary key (ordID)
);

/*==============================================================*/
/* Table: rooms                                                 */
/*==============================================================*/
create table rooms
(
   roomID               numeric(8,0) not null,
   dorName              varchar(255) not null,
   area                 float,
   type                 text,
   price                float,
   state                text,
   primary key (roomID, dorName)
);

/*==============================================================*/
/* Table: visitors                                              */
/*==============================================================*/
create table visitors
(
   visID                numeric(18,0) not null,
   name                 text,
   gender               text not null,
   Tel                  numeric(11,0),
   ordID                numeric(8,0),
   roomID               numeric(8,0),
   dorName              varchar(255),
   primary key (visID)
);

DELIMITER //

CREATE TRIGGER dor_del
AFTER DELETE ON dormitories
FOR EACH ROW
BEGIN
    DELETE FROM rooms
    WHERE `rooms`.`dorName` = OLD.`dorName`; -- 使用反引号确保字段名解析
END //

DELIMITER ;

DELIMITER //

-- 创建存储过程：查询可预订的民宿及房间信息
CREATE PROCEDURE GetAvailableRooms()
BEGIN
    SELECT 
        d.dorName AS '民宿名称',
        d.address AS '地址',
        d.class AS '民宿等级',
        r.roomID AS '房间号',
        r.area AS '面积',
        r.type AS '房型',
        r.price AS '价格'
    FROM 
        dormitories d
    INNER JOIN 
        rooms r ON d.dorName = r.dorName
    WHERE 
        r.state = '可预订'  -- 假设 rooms.state 字段标识可预订状态
    ORDER BY 
        d.dorName, r.roomID;
END //

DELIMITER ;

alter table comments add constraint FK_Reference_10 foreign key (dorName)
      references dormitories (dorName) on delete restrict on update restrict;

alter table host add constraint FK_Reference_11 foreign key (dorName)
      references dormitories (dorName) on delete restrict on update restrict;

alter table orders add constraint FK_Reference_6 foreign key (roomID, dorName)
      references rooms (roomID, dorName) on delete restrict on update restrict;

alter table rooms add constraint FK_Reference_13 foreign key (dorName)
      references dormitories (dorName) on delete restrict on update restrict;

alter table visitors add constraint FK_Reference_12 foreign key (ordID)
      references orders (ordID) on delete restrict on update restrict;

alter table visitors add constraint FK_Reference_7 foreign key (roomID, dorName)
      references rooms (roomID, dorName) on delete restrict on update restrict;

