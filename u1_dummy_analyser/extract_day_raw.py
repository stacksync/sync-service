__author__ = 'sergi'

''' This program extracts an hour of the U1 trace '''

from utils import parse_line
import gzip
import time
import calendar

DATASET_PATH = "/home/lab141/Desktop/Sorted_Trace.gz"
DAY = "2014-01-21 00:00:00.000"
# DAY = "2014-01-11 00:00:00.000"
IMPORTANT_OPERATIONS = set(['AuthenticateResponse', 'MakeResponse', 'MoveResponse', 'Unlink', 'PutContentResponse'])


strptime = time.strptime(DAY, '%Y-%m-%d %H:%M:%S.%f')
initial_trace_time = calendar.timegm(strptime)
final_trace_time = initial_trace_time + (3600 * 24)
day_to_analyse = DAY[:10]

day_analysed = ''
day_seen = False

csv = open('day_raw.csv', 'w')

print >> csv, "time,operation,node_id,node_type,mime,size,user"

start = time.time()
with gzip.open(DATASET_PATH, 'rt') as f:

    # skip the first line
    next(f)
    # continue reading
    for l in f:

        T, addr, caps, client_metadata, current_gen, extension, failed, free_bytes, from_gen, file_hash, \
        level, logfile_id, method, mime, msg, node_id, nodes, pid, req_id, operation, root, server, shared_by, \
        shared_to, shares, sid, size, time_lapse, tstamp, node_type, udfs, user, user_id, vol_id = parse_line(l)

        print str(tstamp)

        # check if the day and the hour start at the same time
        if tstamp.startswith(day_to_analyse):
            day_seen = True
            csv.write(l)
        # gtfo when we overpass the hour to analyse
        elif day_seen:
            break


end = time.time()
print end - start