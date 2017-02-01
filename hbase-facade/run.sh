
# Remove output folder
hdfs dfs -rm output/*
hdfs dfs -rmdir output

# hdfs dfs -rmdir output/_temporary/1

# Run the Hadoop job
#hadoop jar  odmhadoop.jar mapreduce.OdmHadoopAdaptor \
#             input output \
#      -Dcolumn.names=lineNumber,customerId,customerName,stockName,transaction,transactionDate,units,stockPrice,total \
#            -DrulesetVersion=/validatePnrApp/1.0/validatePnr/1.0
#            -Dhost=localhost:9080
#            -DresUser=resAdmin
#            -DresPwd=resAdmin
#            -DresExPwd=resAdmin
#            -DblueMixMode=false
#            -Dhttps=false

export HBASE_HOME=/usr/iop/4.2.0.0/hbase

HBASE_CLASSPATH=$(${HBASE_HOME}/bin/hbase mapredcp | tr ':' ',')

echo $HBASE_CLASSPATH

yarn jar  odmhadoop.jar mapreduce.OdmHadoopAdaptor \
 -libjars=${HBASE_HOME}/conf,${HBASE_CLASSPATH}\
             input output \
             passportNumber,customerName,dateOfBirth,flightNumber,flightDate,route \
             /validatePnrApp/1.0/validatePnr/1.0 \
             brsv2-ae05805f.ng.bluemix.net \
             resAdmin \
             skn9o0rwqng2 \
             Basic cmVzQWRtaW46c2tuOW8wcndxbmcy \
             true

# Display the result
hdfs dfs -ls output
hdfs dfs -cat output/*
