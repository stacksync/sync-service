__author__ = 'sergi'

users = {}
user_node = {}
num_nodes = 10
hour_file = 'hour_parsed.csv'

def remove_users_by_num_commits(array, threshold):
    i = 0
    for x in array:
        if x['ops'] > threshold:
            i += 1
        else:
            break
    for x in xrange(i):
        array.pop(0)

def fill_machines(nodes, array):
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

with open(hour_file, 'r') as f:
    next(f)
    for line in f:
        time, op, node_id, node_type, mime, size, user_id = line.split(",")

        if user_id not in users:
            users[user_id] = {'ops': 0, 'files': {}}

        users[user_id]['ops'] += 1
        files = users[user_id]['files']

        if node_id not in files:
            files[node_id] = 0

        files[node_id] += 1

print "Num users = ", len(users)

list = sorted(users, key=lambda user_id: users[user_id]['ops'], reverse=True)

array = []
for user_id in list:
    array.append({'user_id': user_id, 'ops': users[user_id]['ops']})

nodes = []
file_nodes = []
for i in xrange(num_nodes):
    nodes.append({'ops': 0, 'users': []})
    file_nodes.append(open('node'+str(i)+'.csv','w'))


# print array
# num_users = len(array)
# remove_users_by_num_commits(array, 1000)
# print array
# print num_users-len(array), 'purged from', num_users, '(', len(array), ')'

fill_machines(nodes, array)

with open(hour_file, 'r') as f:
    next(f)
    for line in f:
        time, op, node_id, node_type, mime, size, user_id = line.split(",")

        # if some users are removed from the dict, check they exist!!
        if user_id in user_node:
            file_nodes[user_node[user_id]].write(line)

for f in file_nodes:
    f.close()






