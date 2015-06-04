__author__ = 'sergi'

''' This program extracts an hour of the U1 trace and transforms it into
a file with the following params: timestamp file_id file_type file_mime file_size user_id'''

import gzip
import time
import calendar

'''Parse a raw trace entry'''
def parse_line(l):
    l = l.replace("\"\"","").replace("b\'","").replace("\'","").replace("\n","").replace("\r","")
    while "\"" in l:
        first_ocurrence = l.index("\"")
        second_ocurrence = l[first_ocurrence+1:].index("\"")+2
        l = l[0:first_ocurrence] + l[second_ocurrence+first_ocurrence:]
    return l.split(",")

#DATASET_PATH = "/home/lab141/Desktop/Sorted_Trace.gz"
DATASET_PATH = "/home/sergi/Documents/Feina/trace/one_day_trace.gz"

HOUR = "2014-01-21 13:00:00.000"
IMPORTANT_OPERATIONS = set(['MakeResponse', 'MoveResponse', 'Unlink', 'PutContentResponse'])


strptime = time.strptime(HOUR, '%Y-%m-%d %H:%M:%S.%f')
initial_trace_time = calendar.timegm(strptime)
final_trace_time = initial_trace_time + 3600
hour_to_analyse = HOUR[:13]

hour_analysed = ''
hour_seen = False

csv = open('hour_parsed.csv', 'w')

print >> csv, "timestamp,op,file_id,file_type,file_mime,file_size,user_id"

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
            try:
                struct_time = time.strptime(tstamp[:26], '%Y-%m-%d %H:%M:%S.%f')
                trace_time = calendar.timegm(struct_time)
                t = float(trace_time - initial_trace_time) + float(tstamp[tstamp.index('.'):])
            except ValueError:
                struct_time = time.strptime(tstamp, '%Y-%m-%d %H:%M:%S')
                trace_time = calendar.timegm(struct_time)
                t = float(trace_time - initial_trace_time)

            # check if it's the hour we want to obtain
            if initial_trace_time <= trace_time < final_trace_time:
                if T.startswith('storage') and msg == 'Request done' and operation in IMPORTANT_OPERATIONS:
                    if node_id != '' or operation != 'Unlink':
                        op = ''
                        if operation == 'PutContentResponse':
                            op = 'mod'
                        elif operation == 'MakeResponse':
                            op = 'new'
                        elif operation == 'Unlink':
                            op = 'del'
                        else:
                            op = 'mov'
                        print >> csv, str(t) + "," + op + "," + node_id + "," + node_type + "," + mime + "," + str(size) + ","+ str(user)
                        #print >> csv, str(t) + "," + op + "," + node_id + "," + node_type + "," + mime + "," + str(size) + ","+ str(sid)

            # gtfo when we overpass the hour to analyse
            elif t >= final_trace_time:
                break
        # gtfo when we overpass the hour to analyse
        elif hour_seen:
            break


end = time.time()
print end - start


