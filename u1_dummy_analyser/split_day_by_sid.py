__author__ = 'sergi'

num_nodes = 80 # each node will be a thread
# user = { 'last_node': node_id, 'last_time': last_time}
users = {}
# node = {'id': id1, 'file': file_descriptor}
nodes = []
# file to read
day_file = 'day_parsed_uuid_version.csv'
# seconds
threshold = 60 * 10 # 10 minutes

for i in xrange(num_nodes):
    nodes.append({'id': i, 'file': open('node_split_'+str(i)+'.csv', 'w')})

last_time = 0
with open(day_file, 'r') as f:
    next(f)
    for line in f:
        timestamp, op, file_id, file_type, file_mime, file_size, file_version, sid, user_id = line.split(",")
        time = float(timestamp)

        node = None

        if sid not in users or threshold <= (last_time - users[sid]['last_time']):
            node = nodes.pop(0)
        else:
            node_id = users[sid]['last_node']
            i = 0
            for x in nodes:
                if x['id'] == node_id:
                    break
                i += 1

            node = nodes.pop(i)

        users[sid] = {'last_node': node['id'], 'last_time': time}
        nodes.append(node)
        node['file'].write(line)
        last_time = time

for node in nodes:
    node['file'].close()


