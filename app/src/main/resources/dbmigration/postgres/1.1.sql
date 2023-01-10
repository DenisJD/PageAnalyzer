-- apply changes
alter table url_check alter column description type text using description::text;
