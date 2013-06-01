CREATE TABLE pin (id integer primary key autoincrement, pin varchar(50), redeemed tinyint, note varchar(200));
CREATE TABLE tag(tag varchar(50), pin varchar(50), primary key(tag, pin));