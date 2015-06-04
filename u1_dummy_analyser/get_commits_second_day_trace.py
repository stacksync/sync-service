__author__ = 'sergi'


DAY_PATH = "day_parsed_uuid_version_from12to18.csv"


out = open('commits_second_from12to18_0.dat', 'w')

with open(DAY_PATH, 'r') as f:
    next(f)

    commits = 0
    num_seconds = 1
    seconds = num_seconds

    for l in f:
        timestamp, op, file_id, file_type, file_mime, file_size, version, sid, user_id = l.split(",")

        time = float(timestamp)
        if time < seconds:
                commits += 1
        else:
            out.write(str(seconds - 1)+'\t'+str(commits)+'\n')
            commits = 1
            seconds += num_seconds