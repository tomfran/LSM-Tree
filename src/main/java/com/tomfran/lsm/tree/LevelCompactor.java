package com.tomfran.lsm.tree;

public class LevelCompactor extends BackgroundExecutor {

    LSMTree tree;
    int levelSizeThreshold;

    public LevelCompactor(LSMTree tree, int repeatIntervalMills, int levelSizeThreshold) {
        super(repeatIntervalMills);

        this.tree = tree;
        this.levelSizeThreshold = levelSizeThreshold;

        setRunnable(this::compact);
    }

    private void compact() {
        int levels = tree.ssTables.size();
        for (int i = 0; i < levels; i++) {
            if (tree.ssTables.get(i).size() > levelSizeThreshold) {
                System.out.println(">> COMPACTING LEVEL " + i);
                tree.compact(i);
            }
        }
    }
}
