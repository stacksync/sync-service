__author__ = 'sergi'

COMMIT_OP = ['MakeResponse', 'MoveResponse', 'Unlink', 'PutContentResponse']
SECONDS = 5*60 # five minutes

users = set()
next_time = SECONDS
commits = 0
auth = 0
file_size = 0
num_files = 0

writer = open('summarize.csv', 'w')
print >> writer, "time,commits,auth,users,file_size_mean"

with open('hour.csv', 'r') as f:
    next(f)
    for line in f:
        time, operation, node_id, node_type, mime, size, user = line.split(",")

        if float(time) >= next_time:
            print >> writer, str(next_time-SECONDS) + "," + str(commits) + "," + str(auth) + "," + str(len(users)) + "," + str(file_size/num_files)

            next_time += SECONDS
            commits = 0
            auth = 0
            users = set()

            file_size = 0
            num_files = 0

        if operation in COMMIT_OP:
            commits += 1
            if size != '':
                file_size += int(size)
                num_files += 1
        else:
            auth += 1

        users.add(user)

if commits > 0 or auth > 0:
        print >> writer, str(next_time-SECONDS) + "," + str(commits) + "," + str(auth) + "," + str(len(users)) + "," + str(file_size/num_files)
