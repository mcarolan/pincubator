CREATE TABLE pin (id integer primary key autoincrement, pin varchar(50) not null, redeemed tinyint not null, note varchar(200));
CREATE TABLE tag(id integer primary key autoincrement, tag varchar(50) not null);
CREATE TABLE pin_tag(pin_id integer not null, tag_id integer not null, primary key(pin_id, tag_id), foreign key(pin_id) references pin(id), foreign key (tag_id) references tag(id));