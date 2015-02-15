import os

for x in range(5):
    os.system("python random_ksat.py 3 " + str(2**x * 2000) + " " + str(2**x * 6000) + " 1 rho3/v" \
                + str(2**x * 2000) + "c" + str(2**x * 6000) + ".cnf")
