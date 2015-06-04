__author__ = 'sergi'

''' This program extracts an hour of the U1 trace '''

from utils import parse_line
import gzip
import time
import calendar

DATASET_PATH = "/home/lab141/Desktop/Sorted_Trace.gz"
HOUR = "2014-01-11 07:00:00.000"
IMPORTANT_OPERATIONS = set(['AuthenticateResponse', 'MakeResponse', 'MoveResponse', 'Unlink', 'PutContentResponse'])


strptime = time.strptime(HOUR, '%Y-%m-%d %H:%M:%S.%f')
initial_trace_time = calendar.timegm(strptime)
final_trace_time = initial_trace_time + 3600
hour_to_analyse = HOUR[:13]

hour_analysed = ''
hour_seen = False

csv = open('hour.csv', 'w')

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

        if not hour_analysed.startswith(tstamp[:13]):
            hour_analysed = tstamp[:13]
            print "Analysing hour : " + hour_analysed

        # check if the day and the hour start at the same time
        if tstamp.startswith(hour_to_analyse):
            hour_seen = True
            struct_time = time.strptime(tstamp, '%Y-%m-%d %H:%M:%S.%f')
            trace_time = calendar.timegm(struct_time)
            t = float(trace_time - initial_trace_time) + float(tstamp[tstamp.index('.'):])

            # check if it's the hour we want to obtain
            if initial_trace_time <= trace_time < final_trace_time:
                if T.startswith('storage') and msg == 'Request done' and operation in IMPORTANT_OPERATIONS:
                    if node_id != '' or operation != 'Unlink':
                        # print >> csv, str(t) + "," + operation + "," + node_id + "," + node_type + "," + mime + "," + str(size) + ","+ str(user)
                        print >> csv, str(t) + "," + operation + "," + node_id + "," + node_type + "," + mime + "," + str(size) + ","+ str(sid)

            # gtfo when we overpass the hour to analyse
            elif t >= final_trace_time:
                break
        # gtfo when we overpass the hour to analyse
        elif hour_seen:
            break


end = time.time()
print end - start