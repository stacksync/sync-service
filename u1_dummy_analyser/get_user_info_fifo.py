__author__ = 'sergi'

num_nodes = 10
# user = { 'last_node': node_id, 'last_time': last_time}
users = {}
# node = {'id': id1, 'file': file_descriptor}
nodes = []
# file to read
hour_file = 'hour_parsed.csv'
# seconds
threshold = 10

for i in xrange(num_nodes):
    nodes.append({'id': i, 'file': open('node_fifo_'+str(i)+'.csv', 'w')})

last_time = 0
with open(hour_file, 'r') as f:
    next(f)
    for line in f:
        time, op, node_id, node_type, mime, size, user_id = line.split(",")
        time = float(time)

        node = None

        if user_id not in users or threshold <= (last_time - users[user_id]['last_time']):
            node = nodes.pop(0)
        else:
            node_id = users[user_id]['last_node']
            i = 0
            for x in nodes:
                if x['id'] == node_id:
                    break
                i += 1

            node = nodes.pop(i)

        users[user_id] = {'last_node': node['id'], 'last_time': time}
        nodes.append(node)
        node['file'].write(line)
        last_time = time

for node in nodes:
    node['file'].close()


