// 176


import java.util.*;

class Player {

    /** Pac - static inner class */
    static class Pac {
        int id, r, c, cd;
        char type;
        int zone;
        public Pac(int id, int r, int c, int cd, char type) {
            this.id = id;
            this.r = r;
            this.c = c;
            this.cd = cd;
            this.type = type;
            this.zone = -1; // 初始未分配区域
        }
    }

    // Collision record
    static Set<String> plannedPositions = new HashSet<>();
    static Map<Integer, int[]> zoneCenters = new HashMap<>();

    public static void main(String[] args) {

        // Field
        Scanner in = new Scanner(System.in);
        boolean firstRound = true;
        int width = in.nextInt();
        int height = in.nextInt();
        Map<Integer, int[]> prevPos = new HashMap<>();

        // map: boolean[][] -> false represents wall
        boolean[][] map = new boolean[height][width];
        if (in.hasNextLine()) in.nextLine();
        for (int i = 0; i < height; i++) {
            String line = in.nextLine();
            for (int j = 0; j < width; j++) {
                map[i][j] = (line.charAt(j) != '#');
            }
        }

        // pelletMap: int[][] -> represents pellet
        int[][] pelletMap = new int[height][width];
        for (int i = 0; i < height; i++) Arrays.fill(pelletMap[i], 1);

        // game loop
        while (true) {

            // Collect data- > make choice -> control

            // Important data
            plannedPositions.clear();
            ArrayList<Pac> myPac = new ArrayList<>();
            ArrayList<Pac> opPac = new ArrayList<>();
            ArrayList<int[]> superP = new ArrayList<>();

            // Scores
            in.nextInt();
            in.nextInt();


            // visiblePacCount: int
            // Put pac info in myPac & opPac: ArrayList<Pac>
            // Update pelletMap based on what I collected
            int visiblePacCount = in.nextInt();
            for (int i = 0; i < visiblePacCount; i++) {
                int pacId = in.nextInt();
                boolean mine = in.nextInt() != 0;
                int x = in.nextInt();
                int y = in.nextInt();
                char typeId = in.next().charAt(0);
                in.nextInt(); // speedTurnsLeft
                int abilityCooldown = in.nextInt();

                pelletMap[y][x] = 0;
                Pac tempPac = new Pac(pacId, y, x, abilityCooldown, typeId);

                if (mine) {
                    myPac.add(tempPac);
                    setVisible0(tempPac, map, pelletMap);

                    // MODIFICATION 4: 首回合区域分配
                    if (firstRound) {
                        zoneCenters.put(tempPac.id, new int[]{y, x});
                        tempPac.zone = tempPac.id;
                    }
                } else {
                    opPac.add(tempPac);
                }
            }
            firstRound = false;

            // visiblePelletCount: int
            // fill the pelletMap
            int visiblePelletCount = in.nextInt();
            for (int i = 0; i < visiblePelletCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int value = in.nextInt();
                pelletMap[y][x] = value;
                if (value == 10) superP.add(new int[]{y, x});
            }

            ArrayList<String> commands = new ArrayList<>();
            List<int[]> availableSuperP = new ArrayList<>(superP); // MODIFICATION 5: 动态超级豆列表

            // Making choice
            for (Pac p : myPac) {

                // command
                String command = generateMoveCommand(p, map, pelletMap, availableSuperP, opPac);
                commands.add(command);

                // Record positions
                String[] parts = command.split(" ");
                if (parts[0].equals("MOVE")) {
                    int tx = Integer.parseInt(parts[2]);
                    int ty = Integer.parseInt(parts[3]);
                    plannedPositions.add(ty + "," + tx);
                }
            }

            System.out.println(String.join(" | ", commands));
        }
    }

    /**
     * Generate command based ion given information
     * @param p                  My Pac-man
     * @param map                map
     * @param pellets            pelletMap
     * @param availableSuperP    The ArrayList of available superPellets
     * @param opPacs             The ArrayList of opponent's Pac-man
     * @return                   The command we can print to console
     */
    private static String generateMoveCommand(Pac p, boolean[][] map, int[][] pellets,
                                              List<int[]> availableSuperP, List<Pac> opPacs) {
        // Offence & Defence
        for (Pac op : opPacs) {
            int distance = Math.abs(p.r - op.r) + Math.abs(p.c - op.c);
            if (distance < 2 && op.cd > 1 && p.cd == 0) {
                return switchType(p, op);
            }
            if (distance < 4 && p.cd > 1 && op.cd == 0) {
                return escape(p, op, map);
            }
        }

        // SPEED
        if (p.cd == 0) {
            return "SPEED " + p.id + " TURING";
        }

        // Get localized target
        int[] target = null;
        if (!availableSuperP.isEmpty()) {
            target = findNearestInZone(p, availableSuperP);

            if (target != null) {
                int[] finalTarget = target;
                availableSuperP.removeIf(pos -> Arrays.equals(pos, finalTarget));
                return formatMove(p, target, "Turing");
            }
        }

        // BFS
        target = bfsFindPellet(p, map, pellets, plannedPositions);
        if (target != null) {
            return formatMove(p, target, "TURING");
        }

        // Base condition
        return safeRandomMove(p, map);
    }

    /**
     *
     * @param pac
     * @param map
     * @param pellets
     * @param forbidden
     * @return
     */
    private static int[] bfsFindPellet(Pac pac, boolean[][] map, int[][] pellets, Set<String> forbidden) {
        Queue<int[]> queue = new LinkedList<>();
        boolean[][] visited = new boolean[map.length][map[0].length];
        queue.add(new int[]{pac.r, pac.c});
        visited[pac.r][pac.c] = true;

        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();

            if (forbidden.contains(pos[0]+","+pos[1])) continue;

            if (pellets[pos[0]][pos[1]] > 0) {
                return pos;
            }
            for (int[] d : dirs) {
                int nr = pos[0] + d[0];
                int nc = pos[1] + d[1];
                if (isValid(map, nr, nc) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        return null;
    }


    private static int[] findNearestInZone(Pac pac, List<int[]> targets) {
        int[] nearest = null;
        int minDist = Integer.MAX_VALUE;
        int[] zoneCenter = zoneCenters.get(pac.id);

        for (int[] pos : targets) {

            double zoneWeight = 0.3 * distance(pos, zoneCenter);
            double totalScore = distance(pos, new int[]{pac.r, pac.c}) + zoneWeight;

            if (totalScore < minDist) {
                minDist = (int)totalScore;
                nearest = pos;
            }
        }
        return nearest;
    }

    private static int distance(int[] a, int[] b) {
        return Math.abs(a[0]-b[0]) + Math.abs(a[1]-b[1]);
    }

    private static String safeRandomMove(Pac p, boolean[][] map) {
        List<int[]> validMoves = new ArrayList<>();
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};

        for (int[] d : dirs) {
            int nr = p.r + d[0];
            int nc = p.c + d[1];
            if (isValid(map, nr, nc) && !plannedPositions.contains(nr+","+nc)) {
                validMoves.add(new int[]{nr, nc});
            }
        }

        if (!validMoves.isEmpty()) {
            int[] target = validMoves.get(new Random().nextInt(validMoves.size()));
            return formatMove(p, target, "TURING");
        }
        return "MOVE " + p.id + " " + p.c + " " + p.r + " TURING";
    }


    private static String switchType(Pac pac, Pac op) {
        String type = "";
        if (op.type == 'R') type = "PAPER";
        else if (op.type == 'P') type = "SCISSORS";
        else type = "ROCK";

        int[] interceptPos = predictEnemyMove(op);
        return "SWITCH " + pac.id + " " + type + " MOVE " + interceptPos[1] + " " + interceptPos[0];
    }

    private static int[] predictEnemyMove(Pac op) {
        return new int[]{op.r, op.c};
    }

    private static String formatMove(Pac p, int[] target, String reason) {

        return "MOVE " + p.id + " " + target[1] + " " + target[0] + " " + reason;
    }


    /**
     * Set all the visible pelletMap a pac can see to zero
     * @param p          The pac
     * @param map        The map
     * @param pellets    The pelletMap
     */
    private static void setVisible0(Pac p, boolean[][] map, int[][] pellets) {
        int h = map.length, w = map[0].length;

        // Up
        for (int r = p.r; r >= 0; r--) {
            if (!map[r][p.c]) break;
            pellets[r][p.c] = 0;
        }
        // Down
        for (int r = p.r; r < h; r++) {
            if (!map[r][p.c]) break;
            pellets[r][p.c] = 0;
        }
        // Left
        for (int c = p.c; c >= 0; c--) {
            if (!map[p.r][c]) break;
            pellets[p.r][c] = 0;
        }
        // Right
        for (int c = p.c; c < w; c++) {
            if (!map[p.r][c]) break;
            pellets[p.r][c] = 0;
        }
    }

    /**
     * Determine if it's a valid position given the row and cow
     * @param map    The map
     * @param r      Row
     * @param c      Col
     * @return       True if is a valid position, otherwise false
     */
    private static boolean isValid(boolean[][] map, int r, int c){
        int h = map.length, w = map[0].length;
        return r >= 0 && r < h && c >= 0 && c < w && map[r][c];
    }

    /**
     * When it's too close to opponent, find a way to escape
     * @param p
     * @param op
     * @param map
     * @return
     */
    private static String escape(Pac p, Pac op, boolean[][] map) {

        int pr = p.r, pc = p.c, or = op.r, oc = op.c;

        // Get direction
        int dy = Integer.compare(pr, or);
        int dx = Integer.compare(pc, oc);

        // Get new position
        int tryR = pr + dy;
        int tryC = pc + dx;

        // If works
        if (isValid(map, tryR, tryC)) {
            return "MOVE " + p.id + " " + tryC + " " + tryR + " Turing";
        }
        // Not, try new
        if (isValid(map, tryR + dy, tryC)) {
            int nr = tryR + dy;
            return "MOVE " + p.id + " " + tryC + " " + nr + " Turing";
        }
        if (isValid(map, tryR, tryC + dx)) {
            int nc = tryC + dx;
            return "MOVE " + p.id + " " + nc + " " + tryR + " Turing";
        }
        // Base case
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nr = pr + d[0];
            int nc = pc + d[1];
            if (isValid(map, nr, nc)) {
                return "MOVE " + p.id + " " + nc + " " + nr+ " Turing";
            }
        }
        return null;
    }
}