__author__ = 'sergi'

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

DAY_PATH = "day_raw.csv"
IMPORTANT_OPERATIONS = set(['MakeResponse', 'MoveResponse', 'Unlink', 'PutContentResponse'])

csv = open('day_parsed.csv', 'w')

print >> csv, "timestamp,op,file_id,file_type,file_mime,file_size,sid,user_id"

FIRST_TIME = True

with open(DAY_PATH, 'r') as f:
    next(f)
    for l in f:
        T, addr, caps, client_metadata, current_gen, extension, failed, free_bytes, from_gen, file_hash, \
        level, logfile_id, method, mime, msg, node_id, nodes, pid, req_id, operation, root, server, shared_by, \
        shared_to, shares, sid, size, time_lapse, tstamp, node_type, udfs, user, user_id, vol_id = parse_line(l)

        if FIRST_TIME:
            strptime = time.strptime(tstamp, '%Y-%m-%d %H:%M:%S.%f')
            initial_trace_time = calendar.timegm(strptime)
            FIRST_TIME = False

        struct_time = time.strptime(tstamp, '%Y-%m-%d %H:%M:%S.%f')
        trace_time = calendar.timegm(struct_time)
        t = float(trace_time - initial_trace_time) + float(tstamp[tstamp.index('.'):])

        if T.startswith('storage') and msg == 'Request done' and operation in IMPORTANT_OPERATIONS:
            if node_id != '':
                op = ''
                if operation == 'PutContentResponse':
                    op = 'mod'
                elif operation == 'MakeResponse':
                    op = 'new'
                elif operation == 'Unlink':
                    op = 'del'
                else:
                    op = 'mov'
                print >> csv, str(t) + "," + op + "," + node_id + "," + node_type + "," + mime + "," + str(size) + ","+ str(sid)+ ","+str(user)