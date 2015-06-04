__author__ = 'sergi'

users = {}
hour_file = 'hour_parsed.csv'

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

num_users = len(list) *0.05


for i in xrange(int(num_users)):
    print list.pop(0)