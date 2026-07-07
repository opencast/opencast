The `update_index.sh` script applies new index mappings to existing data. This is achieved by reindexing the data using Opensearch.

## How to run the script

Ensure Opencast is down when running index modifying scripts to avoid any accidental data loss.
First you need to fill out your Opensearch user credentials in `update_index.sh`. Look for the `ES_HOST`, `ES_USER` and `ES_PASS` variables. Then run this command

```shell
bash update_index.sh
```