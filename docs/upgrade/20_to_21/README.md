The `update_index.sh` script will applies new index mappings to existing data. This is achieved by reindexing the data using Opensearch.

## How to run the script

First you need to fill out your Opensearch user credentials in `update_index.sh`. Look for the `ES_HOST`, `ES_USER` and `ES_PASS` variables. Then run this command

```shell
bash update_index.sh
```