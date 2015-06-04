__author__ = 'sergi'

''' This program extracts an hour of the U1 trace and transforms it into
a file with the following params: timestamp file_id file_type file_mime file_size user_id'''

import gzip
import time
import calendar
import datetime as dt

# DATASET_PATH = "/home/lab141/Desktop/Sorted_Trace.gz"
DATASET_PATH = "/home/sergi/Documents/Feina/trace/one_day_trace.gz"
DAY = "2014-01-21"
IMPORTANT_OPERATIONS = set(['MakeResponse', 'MoveResponse', 'Unlink', 'PutContentResponse'])


strptime = time.strptime(DAY, '%Y-%m-%d')
initial_trace_time = calendar.timegm(strptime)
final_trace_time = initial_trace_time + (3600 * 24)

NEXT_DAY = dt.date.fromtimestamp(final_trace_time).strftime('%Y-%m-%d')

print strptime
print initial_trace_time
print final_trace_time
print time.ctime(initial_trace_time)
print time.ctime(final_trace_time)

commits_hour = []
commit_user_hour = []
commits = 0
next_hour = 3600

'''Parse a raw trace entry'''
def parse_line(l):
    l = l.replace("\"\"","").replace("b\'","").replace("\'","").replace("\n","").replace("\r","")
    while "\"" in l:
        first_ocurrence = l.index("\"")
        second_ocurrence = l[first_ocurrence+1:].index("\"")+2
        l = l[0:first_ocurrence] + l[second_ocurrence+first_ocurrence:]
    return l.split(",")

def fill_machines(user_node, nodes, array):
    for i in xrange(0, len(nodes)):
        user = array.pop(0)
        nodes[i]['ops'] += user['ops']
        nodes[i]['users'].append(user['user_id'])
        user_node[user['user_id']] = i

    while len(array) > 0:
        min = nodes[0]
        j = 0
        for i in xrange(1, len(nodes)):
            if min > nodes[i]['ops']:
                min = nodes[i]['ops']
                j = i

        user = array.pop(0)
        nodes[j]['ops'] += user['ops']
        nodes[j]['users'].append(user['user_id'])
        user_node[user['user_id']] = j

def analyse_hour(users):

    print "Num users = ", len(users)
    list = sorted(users, key=lambda user_id: users[user_id]['ops'], reverse=True)
    array = []
    for user_id in list:
        array.append({'user_id': user_id, 'ops': users[user_id]['ops']})

    return array

def check_user(users, user_id, node_id):
    if user_id not in users:
            users[user_id] = {'ops': 0, 'files': {}}

    users[user_id]['ops'] += 1
    files = users[user_id]['files']

    if node_id not in files:
        files[node_id] = 0

    files[node_id] += 1



users = {}
with gzip.open(DATASET_PATH, 'rt') as f:

    # skip the first line
    next(f)
    # continue reading
    for l in f:

        T, addr, caps, client_metadata, current_gen, extension, failed, free_bytes, from_gen, file_hash, \
        level, logfile_id, method, mime, msg, node_id, nodes, pid, req_id, operation, root, server, shared_by, \
        shared_to, shares, sid, size, time_lapse, tstamp, node_type, udfs, user, user_id, vol_id = parse_line(l)

        if tstamp.startswith(DAY):

            if T.startswith('storage') and msg == 'Request done' and operation in IMPORTANT_OPERATIONS and node_id != '':
                print tstamp
                try:
                    struct_time = time.strptime(tstamp[:26], '%Y-%m-%d %H:%M:%S.%f')
                    trace_time = calendar.timegm(struct_time)
                    t = float(trace_time - initial_trace_time) + float(tstamp[tstamp.index('.'):])
                except ValueError:
                    struct_time = time.strptime(tstamp, '%Y-%m-%d %H:%M:%S')
                    trace_time = calendar.timegm(struct_time)
                    t = float(trace_time - initial_trace_time)



                if t < next_hour:
                    commits += 1
                else:
                    hour = (next_hour - 3600) / 3600
                    print "hour :",hour,'analysed'
                    commits_hour.append((hour, commits))

                    while t >= next_hour:
                        next_hour += 3600

                    array = analyse_hour(users)
                    commit_user_hour.append((hour, array))

                    print users

                    users = {}
                    commits = 1

                check_user(users, str(user), node_id)

        elif tstamp.startswith(NEXT_DAY):
            print 'next day:', tstamp
            break

with open('commits_hour.csv', 'w') as f:
    for (x, y) in commits_hour:
        f.write(str(x)+'\t'+str(y)+'\n')


for (x, y) in commit_user_hour:
    with open('commits_user_hour_'+str(x)+'.csv', 'w') as f:
        for user in y:
            f.write(str(user['user_id'])+'\t'+str(user['ops'])+'\n')
