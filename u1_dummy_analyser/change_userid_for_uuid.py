__author__ = 'sergi'

import uuid

DAY_PATH = "day_parsed_less_hours.csv"

csv = open('day_parsed_uuid_from12to18.csv', 'w')
print >> csv, "timestamp,op,file_id,file_type,file_mime,file_size,sid,user_id"

users = {}
sids = set()

with open(DAY_PATH, 'r') as f:
    next(f)
    for l in f:
        timestamp, op, file_id, file_type, file_mime, file_size, sid, user_id = l.split(",")

        # user_id contains \n
        user_id = user_id.rstrip('\n')

        sids.add(sid)

        if user_id not in users:
            users[user_id] = str(uuid.uuid3(uuid.NAMESPACE_DNS, user_id))

        print >> csv, timestamp + "," + op + "," + file_id + "," + file_type + "," + file_mime + "," + file_size + "," + sid + "," + users[user_id]


user_csv = open('day_users_from12to18.csv', 'w')

for key in users:
    user_csv.write(key + "," + users[key] + "\n")

print "num users = " + str(len(users))
print "num sids = " + str(len(sids))