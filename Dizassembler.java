import java.nio.file.*;
import java.io.*;
import java.util.*;

/*
@author Kuznetsov Ilya ilyakuznecov84@gmail.com
*/

/*
args[0] = name ELF file
args[1] name of new file
*/

public class Dizassembler {
    private final static Map<Integer, Integer> adresses = new LinkedHashMap<>();
    private final static ArrayList<Metka> metkaArrayList = new ArrayList<>();

    public static void main(String[] args) {
        HashMap<String, String> regs = new HashMap<>();
        regs.put("00000", "zero");
        regs.put("00001", "ra");
        regs.put("00010", "sp");
        regs.put("00011", "gp");
        regs.put("00100", "tp");
        regs.put("00101", "t0");
        regs.put("00110", "t1");
        regs.put("00111", "t2");
        regs.put("01000", "s0");
        regs.put("01001", "s1");
        regs.put("01010", "a0");
        regs.put("01011", "a1");
        regs.put("01100", "a2");
        regs.put("01101", "a3");
        regs.put("01110", "a4");
        regs.put("01111", "a5");
        regs.put("10000", "a6");
        regs.put("10001", "a7");
        regs.put("10010", "s2");
        regs.put("10011", "s3");
        regs.put("10100", "s4");
        regs.put("10101", "s5");
        regs.put("10110", "s6");
        regs.put("10111", "s7");
        regs.put("11000", "s8");
        regs.put("11001", "s9");
        regs.put("11010", "s10");
        regs.put("11011", "s11");
        regs.put("11100", "t3");
        regs.put("11101", "t4");
        regs.put("11110", "t5");
        regs.put("11111", "t6");
        int strtab_offset = 0;
        try {
            Path path = Paths.get(args[0]);
            BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
            try {
                byte[] cur = Files.readAllBytes(path);
                int[] mas = new int[cur.length];
                for (int i = 0; i < mas.length; i++) {
                    mas[i] = cur[i] & 0xff;
                }
                if (mas[4] != 1) {
                    throw new RuntimeException("Is not 32 bit");
                }
                if (mas[18] + mas[19] * 256 != 0x00f3) {
                    throw new RuntimeException("Is not RISCV");
                }
                int e_shoff = (int) (mas[35] * Math.pow(256, 3) + mas[34] * Math.pow(256, 2) + mas[33] * Math.pow(256, 1) + mas[32]);
                int e_shstrndx = (mas[51] * 256 + mas[50]);
                int e_shnum = (mas[49] * 256 + mas[48]);
                int e_shentsize = 40;
                int[] segment = new int[e_shentsize];
                int a = e_shoff + e_shentsize * e_shstrndx + 16;
                int offset = mas[a] + mas[a + 1] * 256 + mas[a + 2] * 256 * 256 + mas[a + 3] * 256 * 256 * 256;
                //------------------------------------------------------------------------------------------------------
                //strtab
                //------------------------------------------------------------------------------------------------------
                for (int i = 1; i < e_shnum; i++) {
                    for (int j = 0; j < e_shentsize; j++) {
                        segment[j] = mas[e_shoff + (e_shentsize) * i + j];
                    }
                    int type = segment[4] + segment[5] * 256 + segment[6] * 256 * 256 + segment[7] * 256 * 256 * 256;
                    if (type == 1 || type == 2 || type == 3) {
                        int name = segment[0] + segment[1] * 256 + segment[2] * 256 * 256 + segment[3] * 256 * 256 * 256;
                        name += offset;
                        StringBuilder sb = new StringBuilder();
                        while (mas[name] != 0) {
                            sb.append((char) (mas[name]));
                            name++;
                        }
                        if (sb.toString().equals(".strtab")) {
                            strtab_offset = segment[16] + segment[17] * 256 + segment[18] * 256 * 256 + segment[19] * 256 * 256 * 256;
                        }
                    }
                }
                //------------------------------------------------------------------------------------------------------
                //symtab
                //------------------------------------------------------------------------------------------------------
                for (int i = 1; i < e_shnum; i++) {
                    for (int j = 0; j < e_shentsize; j++) {
                        segment[j] = mas[e_shoff + (e_shentsize) * i + j];
                    }
                    int type = segment[4] + segment[5] * 256 + segment[6] * 256 * 256 + segment[7] * 256 * 256 * 256;
                    if (type == 1 || type == 2 || type == 3) {
                        int name = segment[0] + segment[1] * 256 + segment[2] * 256 * 256 + segment[3] * 256 * 256 * 256;
                        name += offset;
                        StringBuilder sb = new StringBuilder();
                        while (mas[name] != 0) {
                            sb.append((char) (mas[name]));
                            name++;
                        }
                        if (sb.toString().equals(".symtab")) {
                            int e_offset = segment[16] + segment[17] * 256 + segment[18] * 256 * 256 + segment[19] * 256 * 256 * 256;
                            int e_size = segment[20] + segment[21] * 256 + segment[22] * 256 * 256 + segment[23] * 256 * 256 * 256;
                            int[] masOfCommands = new int[e_size];
                            System.arraycopy(mas, e_offset, masOfCommands, 0, e_size);
                            int curNum = 0;
                            for (int x = 0; x < masOfCommands.length; x += 16) {
                                String curName;
                                int nameoffset = masOfCommands[x] + masOfCommands[x + 1] * 256 + masOfCommands[x + 2] * 256 * 256 + masOfCommands[x + 3] * 256 * 256 * 256;
                                StringBuilder namee = new StringBuilder();
                                while (mas[strtab_offset + nameoffset] != 0) {
                                    namee.append((char) mas[strtab_offset + nameoffset]);
                                    nameoffset++;
                                }
                                curName = namee.toString();
                                int curValue, curSize, curType, curBind, curVis, curIndex;
                                curValue = masOfCommands[x + 4] + masOfCommands[x + 5] * 256 + masOfCommands[x + 6] * 256 * 256 + masOfCommands[x + 7] * 256 * 256 * 256;
                                curSize = masOfCommands[x + 8] + masOfCommands[x + 9] * 256 + masOfCommands[x + 10] * 256 * 256 + masOfCommands[x + 11] * 256 * 256 * 256;
                                curType = masOfCommands[x + 12] & 0xf;
                                curBind = (masOfCommands[x + 12]) >> 4;
                                curVis = masOfCommands[x + 13];
                                curIndex = (masOfCommands[x + 14]) + (masOfCommands[x + 15]) * 256;
                                metkaArrayList.add(new Metka(curNum, curValue, curSize, curType, curBind, curVis, curIndex, curName));
                                curNum++;
                            }

                        }
                    }
                }

                //------------------------------------------------------------------------------------------------------
                //находим неиспользуемые метки
                //------------------------------------------------------------------------------------------------------

                ArrayList<Integer> cheto = new ArrayList<>();
                for (int i = 1; i < e_shnum; i++) {
                    for (int j = 0; j < e_shentsize; j++) {
                        segment[j] = mas[e_shoff + (e_shentsize) * i + j];
                    }
                    int type = segment[4] + segment[5] * 256 + segment[6] * 256 * 256 + segment[7] * 256 * 256 * 256;
                    if (type == 1 || type == 2 || type == 3) {
                        int name = segment[0] + segment[1] * 256 + segment[2] * 256 * 256 + segment[3] * 256 * 256 * 256;
                        name += offset;
                        StringBuilder sb = new StringBuilder();
                        while (mas[name] != 0) {
                            sb.append((char) (mas[name]));
                            name++;
                        }
                        if (sb.toString().equals(".text")) {
                            int e_address = segment[12] + segment[13] * 256 + segment[14] * 256 * 256 + segment[15] * 256 * 256 * 256;
                            int e_offset = segment[16] + segment[17] * 256 + segment[18] * 256 * 256 + segment[19] * 256 * 256 * 256;
                            int e_size = segment[20] + segment[21] * 256 + segment[22] * 256 * 256 + segment[23] * 256 * 256 * 256;
                            int[] masOfCommands = new int[e_size];
                            System.arraycopy(mas, e_offset, masOfCommands, 0, e_size);
                            for (int x = 0; x < masOfCommands.length; x += 4) {
                                int command = masOfCommands[x] + masOfCommands[x + 1] * 256 + masOfCommands[x + 2] * 256 * 256 + masOfCommands[x + 3] * 256 * 256 * 256;
                                int operation = command & 0b1111111;
                                if (operation == 0b1101111) {
                                    int curAddress = x + e_address + signExtend((((command >> 31) & 0b1) << 20) | (((command >> 21) & (0b1111111111)) << 1) | (((command >> 20) & 0b1) << 11) | (((command >> 12) & (0b11111111)) << 12), 20);
                                    for (Metka metka : metkaArrayList) {
                                        if ((metka.value != curAddress) && metka.type == 2) {
                                            if (!cheto.contains(curAddress)) {
                                                cheto.add(curAddress);
                                            }
                                        }
                                    }
                                } else if (operation == 0b1100011) {
                                    int curAddress = x + e_address + signExtend((((command >> 31) & 0b1) << 12) | (((command >> 25) & (0b111111)) << 5) | (((command >> 8) & (0b1111)) << 1) | (((command >> 7) & 0b1) << 11), 12);
                                    for (int el = 0; el < metkaArrayList.size(); el++) {
                                        if (!cheto.contains(curAddress)) {
                                            cheto.add(curAddress);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Integer[] sorted = cheto.toArray(new Integer[0]);
                Arrays.sort(sorted);
                for (int i = 0; i < sorted.length; i++) {
                    adresses.put(sorted[i], i);
                }
                //--------------------------------------------------------------------------------------------------------
                //непосредственно команды
                //---------------------------------------------------------------------------------------------------------
                writer.write(".text");
                writer.newLine();
                for (int i = 1; i < e_shnum; i++) {
                    for (int j = 0; j < e_shentsize; j++) {
                        segment[j] = mas[e_shoff + (e_shentsize) * i + j];
                    }
                    int type = segment[4] + segment[5] * 256 + segment[6] * 256 * 256 + segment[7] * 256 * 256 * 256;
                    if (type == 1 || type == 2 || type == 3) {
                        int name = segment[0] + segment[1] * 256 + segment[2] * 256 * 256 + segment[3] * 256 * 256 * 256;
                        name += offset;
                        StringBuilder sb = new StringBuilder();
                        while (mas[name] != 0) {
                            sb.append((char) (mas[name]));
                            name++;
                        }

                        if (sb.toString().equals(".text")) {
                            int e_address = segment[12] + segment[13] * 256 + segment[14] * 256 * 256 + segment[15] * 256 * 256 * 256;
                            int e_offset = segment[16] + segment[17] * 256 + segment[18] * 256 * 256 + segment[19] * 256 * 256 * 256;
                            int e_size = segment[20] + segment[21] * 256 + segment[22] * 256 * 256 + segment[23] * 256 * 256 * 256;
                            int[] masOfCommands = new int[e_size];
                            System.arraycopy(mas, e_offset, masOfCommands, 0, e_size);
                            for (int x = 0; x < masOfCommands.length; x += 4) {
                                writer.write(String.format("%08x ", e_address + x));
                                int command = masOfCommands[x] + masOfCommands[x + 1] * 256 + masOfCommands[x + 2] * 256 * 256 + masOfCommands[x + 3] * 256 * 256 * 256;
                                boolean has = false;
                                for (Metka metka : metkaArrayList) {
                                    if (metka.value == e_address + x && metka.type == 2) {
                                        writer.write(String.format("%10s: ", metka.name));
                                        has = true;
                                        break;
                                    }
                                }
                                if (!has) {
                                    for (int el = 0; el < metkaArrayList.size(); el++) {
                                        if (adresses.containsKey(e_address + x)) {
                                            writer.write(String.format("LOC_%05X: ", adresses.get(e_address + x)));
                                            has = true;
                                            break;
                                        }
                                    }
                                }
                                if (!has) {
                                    writer.write(String.format("%10s", ""));
                                }
                                int operation = command & 0b1111111;
                                int rd = command >> 7 & 0b11111;
                                int bytes3 = command >> 12 & 0b111;
                                int rs1 = command >> 15 & 0b11111;
                                int rs2 = command >> 20 & 0b11111;
                                int imm110 = command >> 20 & 0b111111111111;
                                int bytes7 = command >> 25;
                                int offsett;
                                String str = "";
                                switch (operation) {
                                    case 0b0110111:
                                        writer.write(String.format("lui %s, %s%n", regs.get(bin(rd, 5)), Integer.toUnsignedString((command >> 12))));
                                        break;
                                    case 0b0010111:
                                        writer.write(String.format("auipc %s, %s%n", regs.get(bin(rd, 5)), Integer.toUnsignedString((command >> 12))));
                                        break;
                                    case 0b1101111:
                                        offsett = signExtend((((command >> 31) & 0b1) << 20) | (((command >> 21) & (0b1111111111)) << 1) | (((command >> 20) & 0b1) << 11) | (((command >> 12) & (0b11111111)) << 12), 20);
                                        writer.write(String.format("jal %s, %d, %s%n", regs.get(bin(rd, 5)), offsett, getMetka(e_address + x + offsett)));
                                        break;
                                    case 0b1100111:
                                        if (bytes3 == 0) {
                                            imm110 = signExtend(imm110, 11);
                                            writer.write(String.format("jalr %s, %d(%s)%n", regs.get(bin(rd, 5)), imm110, regs.get(bin(rs1, 5))));
                                            break;
                                        }
                                    case 0b1100011:
                                        offsett = signExtend((((command >> 31) & 0b1) << 12) | (((command >> 25) & (0b111111)) << 5) | (((command >> 8) & (0b1111)) << 1) | (((command >> 7) & 0b1) << 11), 12);
                                        switch (bytes3) {
                                            case 0b000 -> str = "beq";
                                            case 0b001 -> str = "bne";
                                            case 0b100 -> str = "blt";
                                            case 0b101 -> str = "bge";
                                            case 0b110 -> str = "bltu";
                                            case 0b111 -> str = "bgeu";
                                        }
                                        writer.write(String.format("%s %s, %s, %d, %s %n", str, regs.get(bin(rs1, 5)), regs.get(bin(rs2, 5)), offsett, getMetka(e_address + x + offsett)));
                                        break;
                                    case 0b0000011:
                                        str = switch (bytes3) {
                                            case 0b000 -> "lb";
                                            case 0b001 -> "lh";
                                            case 0b010 -> "lw";
                                            case 0b100 -> "lbu";
                                            case 0b101 -> "lhu";
                                            default -> str;
                                        };
                                        writer.write(String.format("%s %s, %d(%s)%n", str, regs.get(bin(rd, 5)), signExtend(imm110, 11), regs.get(bin(rs1, 5))));
                                        break;
                                    case 0b0100011:
                                        str = switch (bytes3) {
                                            case 0b000 -> "sb";
                                            case 0b001 -> "sh";
                                            case 0b010 -> "sw";
                                            default -> str;
                                        };
                                        int imm115 = rd | ((imm110 >> 5) << 5);
                                        writer.write(String.format("%s %s, %d(%s)%n", str, regs.get(bin(rs2, 5)), signExtend(imm115, 11), regs.get(bin(rs1, 5))));
                                        break;
                                    case 0b0010011:
                                        if (bytes3 == 0b001) {
                                            writer.write(String.format("%s %s, %s, %d%n", "slli", regs.get(bin(rd, 5)), regs.get(bin(rs1, 5)), imm110));
                                        } else if (bytes3 == 0b101) {
                                            if (bytes7 == 0b0100000) {
                                                writer.write(String.format("%s %s, %s, %d%n", "srai", regs.get(bin(rd, 5)), regs.get(bin(rs1, 5)), imm110 & 0b11111));
                                            } else {
                                                writer.write(String.format("%s %s, %s, %d%n", "srli", regs.get(bin(rd, 5)), regs.get(bin(rs1, 5)), imm110));
                                            }
                                        } else {
                                            str = switch (bytes3) {
                                                case 0b000 -> "addi";
                                                case 0b010 -> "slti";
                                                case 0b011 -> "sltiu";
                                                case 0b100 -> "xori";
                                                case 0b110 -> "ori";
                                                case 0b111 -> "andi";
                                                default -> str;
                                            };
                                            imm110 = signExtend(imm110, 11);
                                            writer.write(String.format("%s %s, %s, %d%n", str, regs.get(bin(rd, 5)), regs.get(bin(rs1, 5)), imm110));
                                        }
                                        break;
                                    case 0b0110011:
                                        if (bytes7 == 0b0100000) {
                                            str = switch (bytes3) {
                                                case 0b000 -> "sub";
                                                case 0b101 -> "sra";
                                                default -> str;
                                            };
                                            writer.write(String.format("%s %s, %s, %s%n", str, regs.get(bin(rd, 5)), regs.get(bin(rs1, 5)), regs.get(bin(rs2, 5))));
                                        } else if (bytes7 == 0) {
                                            str = switch (bytes3) {
                                                case 0b000 -> "add";
                                                case 0b001 -> "sll";
                                                case 0b010 -> "slt";
                                                case 0b011 -> "sltu";
                                                case 0b100 -> "xor";
                                                case 0b101 -> "srl";
                                                case 0b110 -> "or";
                                                case 0b111 -> "and";
                                                default -> str;
                                            };
                                            writer.write(String.format("%s %s, %s, %s%n", str, regs.get(bin(rd, 5)), regs.get(bin(rs1, 5)), regs.get(bin(rs2, 5))));
                                        } else if (bytes7 == 1) {
                                            str = switch (bytes3) {
                                                case 0b000 -> "mul";
                                                case 0b001 -> "mulh";
                                                case 0b010 -> "mulhsu";
                                                case 0b011 -> "mulhu";
                                                case 0b100 -> "div";
                                                case 0b101 -> "divu";
                                                case 0b110 -> "rem";
                                                case 0b111 -> "remu";
                                                default -> str;
                                            };
                                            writer.write(String.format("%s %s, %s, %s%n", str, regs.get(bin(rd, 5)), regs.get(bin(rs1, 5)), regs.get(bin(rs2, 5))));
                                        }
                                        break;
                                    case 0b1110011:
                                        if (bytes3 == 0) {
                                            if (imm110 == 0) {
                                                writer.write(String.format("%s%n", "ecall"));
                                            } else if (imm110 == 1) {
                                                writer.write(String.format("%s%n", "ebreak"));
                                            } else {
                                                writer.write("unknown_command\n");
                                            }
                                        } else {
                                            str = switch (bytes3) {
                                                case 0b000 -> "";
                                                case 0b001 -> "csrrw";
                                                case 0b010 -> "csrrs";
                                                case 0b011 -> "csrrc";
                                                case 0b101 -> "csrrwi";
                                                case 0b110 -> "csrrsi";
                                                case 0b111 -> "csrrci";
                                                default -> str;
                                            };
                                            writer.write(String.format("%6s %s, %s, %s%n", str, regs.get(bin(rd, 5)), imm110, regs.get(bin(rs1, 5))));
                                        }
                                        break;
                                    default:
                                        writer.write("unknown_command\n");

                                }
                            }
                        }
                    }
                }
                writer.newLine();
                writer.write(".symtab");
                writer.newLine();
                writer.write(String.format("%s %-15s %7s %-8s %-8s %-8s %6s %s\n", "Symbol", "Value", "Size", "Type", "Bind", "Vis", "Index", "Name"));
                for (Metka metka : metkaArrayList) {
                    writer.write(metka.toString());
                }
            } finally {
                writer.close();
            }
            System.out.println("Program finished with no exceptions");
        } catch (IOException e) {
            System.out.println("Invalid elf file: " + e.getMessage());
        }
    }

    private static String hex(int n) {
        return Integer.toUnsignedString(n, 16);
    }

    private static String bin(int n, int count) {
        StringBuilder ans = new StringBuilder();
        for (int i = 0; i < count; i++) {
            ans.append(n % 2);
            n /= 2;
        }
        return ans.reverse().toString();
    }

    private static int signExtend(int n, int offset) {
        if (((n >> offset) & 0b1) == 0) {
            return n;
        }
        return -(-n & ((1 << offset) - 1));
    }

    public static class Metka {
        private final int num;
        private final int value;
        private final int size;
        private final int type;
        private final int bind;
        private final int vis;
        private final int index;
        private final String name;

        private Metka(int num, int value, int size, int type, int bind, int vis, int index, String name) {
            this.num = num;
            this.value = value;
            this.size = size;
            this.type = type;
            this.bind = bind;
            this.vis = vis;
            this.index = index;
            this.name = name;
        }

        private String getType(int type) {
            return switch (type) {
                case 0 -> "NOTYPE";
                case 1 -> "OBJECT";
                case 2 -> "FUNC";
                case 3 -> "SECTION";
                case 4 -> "FILE";
                case 13 -> "LOPROC";
                case 15 -> "HIPROC";
                default -> "";
            };
        }

        private String getBind(int bind) {
            return switch (bind) {
                case 0 -> "LOCAL";
                case 1 -> "GLOBAL";
                case 2 -> "WEAK";
                case 13 -> "LOPROC";
                case 15 -> "HIPROC";
                default -> "";
            };
        }

        private String getVis(int vis) {
            return switch (vis) {
                case 0 -> "DEFAULT";
                case 1 -> "INTERNAL";
                case 2 -> "HIDDEN";
                case 3 -> "PROTECTED";
                default -> "";
            };
        }

        private String getIndex(int index) {
            switch (index) {
                case 0:
                    return "UNDEF";
                case 0xfff1:
                    return "ABS";
                case 0xfff2:
                    return "COMMON";
                default:
                    if (0xff00 <= index && index <= 0xffff) {
                        return "RESERVED";
                    } else {
                        return String.valueOf(index);
                    }
            }
        }

        @Override
        public String toString() {
            return String.format("[%4d] 0x%-15s %5d %-8s %-8s %-8s %6s %s\n", num, hex(value), size, getType(type), getBind(bind), getVis(vis), getIndex(index), name);
        }
    }

    private static String getMetka(int addres) {
        for (Metka metka : metkaArrayList) {
            if (metka.value == addres) {
                return metka.name;
            }
        }
        return String.format("LOC_%05x", adresses.get(addres));
    }
}
