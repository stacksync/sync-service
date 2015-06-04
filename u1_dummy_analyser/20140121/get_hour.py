__author__ = 'sergi'

''' This program extracts an hour of the U1 trace and transforms it into
a file with the following params: timestamp file_id file_type file_mime file_size user_id'''

import gzip
import time
import calendar
import datetime as dt

hour_file = open('hour.csv', 'w')

# DATASET_PATH = "/home/lab141/Desktop/Sorted_Trace.gz"
DATASET_PATH = "/home/sergi/Documents/Feina/trace/one_day_trace.gz"
HOUR = "2014-01-21 13"
NEXT_HOUR = "2014-01-21 14"
IMPORTANT_OPERATIONS = set(['MakeResponse', 'MoveResponse', 'Unlink', 'PutContentResponse'])

'''Parse a raw trace entry'''
def parse_line(l):
    l = l.replace("\"\"","").replace("b\'","").replace("\'","").replace("\n","").replace("\r","")
    while "\"" in l:
        first_ocurrence = l.index("\"")
        second_ocurrence = l[first_ocurrence+1:].index("\"")+2
        l = l[0:first_ocurrence] + l[second_ocurrence+first_ocurrence:]
    return l.split(",")


commits = 0

with gzip.open(DATASET_PATH, 'rt') as f:

    # skip the first line
    next(f)
    # continue reading
    for l in f:

        T, addr, caps, client_metadata, current_gen, extension, failed, free_bytes, from_gen, file_hash, \
        level, logfile_id, method, mime, msg, node_id, nodes, pid, req_id, operation, root, server, shared_by, \
        shared_to, shares, sid, size, time_lapse, tstamp, node_type, udfs, user, user_id, vol_id = parse_line(l)

        if tstamp.startswith(HOUR):
            if T.startswith('storage') and msg == 'Request done' and operation in IMPORTANT_OPERATIONS and node_id != '':
                hour_file.write(l)
                commits += 1
        elif tstamp.startswith(NEXT_HOUR):
            print 'next day:', tstamp
            break


print str(commits)