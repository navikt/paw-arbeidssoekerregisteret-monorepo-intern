CREATE index hendelser_data_idx ON hendelser USING GIN (data jsonb_path_ops);
