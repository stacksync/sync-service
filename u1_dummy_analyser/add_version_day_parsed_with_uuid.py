__author__ = 'sergi'

DAY_PATH = "day_parsed_uuid_from12to18.csv"

csv = open('day_parsed_uuid_version_from12to18.csv', 'w')
day_files_without_new = open('day_files_without_new_from12to18.csv', 'w')

print >> csv, "timestamp,op,file_id,file_type,file_mime,file_size,file_version,sid,user_id"
print >> day_files_without_new, "file_id,user_id"

users = {}
files = {}

i = 1
with open(DAY_PATH, 'r') as f:
    next(f)
    for l in f:
        timestamp, op, file_id, file_type, file_mime, file_size, sid, user_id = l.split(",")

        # user_id contains \n
        user_id = user_id.rstrip('\n')

        if user_id not in users:
            users[user_id] = {'uuid': user_id, 'files': {}}

        file_id_plus_user_id = str(file_id) + str(user_id)

        if file_id_plus_user_id not in files:
            files[file_id_plus_user_id] = i
            i += 1

        user = users[user_id]

        if file_id not in user['files']:
            if op == "new":
                user['files'][file_id] = 1
            else:
                user['files'][file_id] = 2
                print >> day_files_without_new, str(files[file_id_plus_user_id]) + "," + user_id
        elif op != "new":
            user['files'][file_id] += 1

        version = str(user['files'][file_id])

        print >> csv, timestamp + "," + op + "," + str(files[file_id_plus_user_id]) + "," + file_type + "," + file_mime + "," + file_size + "," + version + "," + sid + "," + user_id


