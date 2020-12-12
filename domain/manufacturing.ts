import { Item } from "./item";

/**
 * 制造
 */
export class Manufacturing {

  constructor(
    /**
     * 耗时
     */
    public time: number,

    /**
     * 制造材料需求
     */
    public materials: Item[],

    /**
     * 产品
     */
    public product: Item
  ) {

  }

}