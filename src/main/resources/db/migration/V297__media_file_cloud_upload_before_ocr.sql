drop index media_files_cloud_upload_candidate_idx;

create index media_files_cloud_upload_candidate_idx on media_files(created_at, id)
    where cloud_file_name is null
        and cloud_upload_deadline is null
        and file_name is not null
        and usage_count > 0;
