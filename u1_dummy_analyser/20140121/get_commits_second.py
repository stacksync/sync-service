__author__ = 'sergi'

num_files = 10

for i in xrange(num_files):
    with open('node'+str(i)+'.csv', 'r') as f:

        out = open('node_user_id_'+str(i)+'.dat', 'w')

        num_seconds = 10
        seconds = num_seconds
        commits = 0
        for line in f:
            time, operation, node_id, node_type, mime, size, user = line.split(",")
            time = float(time)

            if time < seconds:
                commits += 1
            else:
                out.write(str(seconds)+'\t'+str(commits)+'\n')
                commits = 1
                seconds += num_seconds

        out.close()
    out.close()






# # with open('hour_parsed.csv', 'r') as f:
# with open('node0.csv', 'r') as f:
#
#     next(f)
#
#     # out = open('hour_parsed.dat', 'w')
#     out = open('node0.dat', 'w')
#
#     num_seconds = 10
#     seconds = num_seconds
#     commits = 0
#     for line in f:
#         time, operation, node_id, node_type, mime, size, user = line.split(",")
#         time = float(time)
#
#         if time < seconds:
#             commits += 1
#         else:
#             out.write(str(seconds)+'\t'+str(commits)+'\n')
#             commits = 1
#             seconds += num_seconds
#
#     out.close()