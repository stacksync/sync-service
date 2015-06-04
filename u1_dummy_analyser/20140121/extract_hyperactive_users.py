__author__ = 'sergi'

USER_SELECTED = str(2147483647)
hour_file = 'hour.csv'

'''Parse a raw trace entry'''
def parse_line(l):
    l = l.replace("\"\"","").replace("b\'","").replace("\'","").replace("\n","").replace("\r","")
    while "\"" in l:
        first_ocurrence = l.index("\"")
        second_ocurrence = l[first_ocurrence+1:].index("\"")+2
        l = l[0:first_ocurrence] + l[second_ocurrence+first_ocurrence:]
    return l.split(",")


csv = open(str(USER_SELECTED)+'.csv', 'w')

num_commits = 0
num_mod = 0
num_del = 0
num_new = 0
num_mov = 0

mime_set = set()
sid_set = set()


with open(hour_file, 'r') as f:

    for line in f:
        T, addr, caps, client_metadata, current_gen, extension, failed, free_bytes, from_gen, file_hash, \
        level, logfile_id, method, mime, msg, node_id, nodes, pid, req_id, operation, root, server, shared_by, \
        shared_to, shares, sid, size, time_lapse, tstamp, node_type, udfs, user, user_id, vol_id = parse_line(line)

        if user == USER_SELECTED:
            csv.write(line)
            mime_set.add(mime)
            sid_set.add(sid)
            num_commits += 1
            if operation == 'PutContentResponse':
                num_mod += 1
            elif operation == 'MakeResponse':
                num_new += 1
            elif operation == 'Unlink':
                num_del += 1
            else:
                num_mov += 1

csv.close()

csv = open(str(USER_SELECTED)+'.txt', 'w')

print >> csv, 'Num commits ' + str(num_commits)
print >> csv, 'Num mod ' + str(num_mod)
print >> csv, 'Num del ' + str(num_del)
print >> csv, 'Num new ' + str(num_new)
print >> csv, 'Num mov ' + str(num_mov)
print >> csv, 'Num sid' + str(len(sid_set))
print >> csv, 'Num different mimes: ' + str(len(mime_set))
for x in mime_set:
    print >> csv, '\t' + x

csv.close()

print str(num_commits), str(num_mod), str(num_del), str(num_new), str(num_mov)
print str(len(mime_set)), mime_set
print str(len(sid_set))

# 2147483647
#
# 1823978021
#
# 1754737504
#
# 94373242
#
# 1092502863