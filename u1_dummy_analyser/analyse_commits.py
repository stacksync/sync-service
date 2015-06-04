__author__ = 'sergi'

import math

users = {}
user_node = {}
num_nodes = 10
hour_file = 'hour_parsed.csv'

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

def write_num_commits(file_name, array):
    with open(file_name, 'w') as f:
        for x in array:
            f.write(str(x['ops']) + '\n')


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

print array

write_num_commits('hour_commits.dat', array)

commits = 0
threshold = 1000
more_than_threshold = 0
for x in array:
    commits += x['ops']
    if x['ops'] > threshold:
        more_than_threshold += 1

print 'num commits = ', commits
print 'num users = ', len(array)
print 'mean = ', commits/len(array)
print 'More than ', threshold, ' = ', more_than_threshold

mean = commits/len(array)
sum = 0
for x in array:
    x = x['ops']
    sum += (x-mean)*(x-mean)

print "Dev = ", math.sqrt(sum/len(array))

nodes = []
for i in xrange(num_nodes):
    nodes.append({'ops': 0, 'users': []})

fill_machines(nodes, array)








