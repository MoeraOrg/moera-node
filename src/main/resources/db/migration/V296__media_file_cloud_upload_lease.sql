alter table media_files add column cloud_upload_deadline timestamp without time zone;

create index media_files_cloud_upload_candidate_idx on media_files(created_at, id)
    where cloud_file_name is null
        and cloud_upload_deadline is null
        and file_name is not null
        and usage_count > 0
        and (recognize_at is null or recognized_at is not null);

create index media_files_cloud_upload_deadline_idx on media_files(cloud_upload_deadline)
    where cloud_file_name is null and cloud_upload_deadline is not null;
