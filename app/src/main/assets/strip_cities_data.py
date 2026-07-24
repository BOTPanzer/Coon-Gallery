import gzip

input_file = "cities500.txt"
output_file = "cities500_compact.bin"

processed_count = 0

with open(input_file, "r", encoding="utf-8") as f_in:
    with gzip.open(output_file, "wt", encoding="utf-8") as f_out:
        for line in f_in:
            tokens = line.split("\t")
            if len(tokens) >= 11:
                name = tokens[1]
                lat = tokens[4]
                lng = tokens[5]
                country = tokens[8]
                admin1 = tokens[10]

                # Write compact tab-separated line: Name \t Lat \t Lng \t Country \t Admin1
                f_out.write(f"{name}\t{lat}\t{lng}\t{country}\t{admin1}\n")
                processed_count += 1

print(f"Done! Compressed {processed_count} cities into '{output_file}'.")