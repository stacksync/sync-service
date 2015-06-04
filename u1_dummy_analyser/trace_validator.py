__author__ = 'sergi'

users_file = "day_users_from12to18.csv"
users_ids = set()
with open(users_file, 'r') as f:
    for line in f:
        tmp_id, user_id = line.split(",")
        if tmp_id in users_ids:
            print "Error with tmp_id ", tmp_id
        else:
            users_ids.add(tmp_id)

print "yo hooo"

items_file = "day_files_without_new_from12to18.csv"
items_ids = set()
repeated_items = 0
with open(items_file, 'r') as f:
    next(f)
    for line in f:
        tmp_id, user_id = line.split(",")
        if tmp_id in items_ids:
            print "Error with tmp_id ", tmp_id
            repeated_items += 1
        else:
            items_ids.add(tmp_id)

print "yo hooo"
print "Repeated items: ", str(repeated_items)

# items_file = "day_files_without_new_from12to18.csv"
# items_ids = set()
# repeated_items = 0
# with open(items_file, 'r') as f:
#     next(f)
#     for line in f:
#         tmp_id, user_id = line.split(",")
#         if tmp_id in items_ids:
#             print "Error with tmp_id ", tmp_id
#             repeated_items += 1
#         else:
#             items_ids.add(tmp_id)
#
# print "yo hooo"
# print "Repeated items: ", str(repeated_items)