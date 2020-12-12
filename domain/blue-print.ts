import { Type } from "./type";

/**
 * 蓝图
 */
export class BluePrint extends Type {

  constructor(
    public id: number,
    public name: string,
    
    /**
     * 产品数量
     */
    public maxProductionLimit: number,

    /**
     * 制造相关
     */
    public manufacturing: any
  ) {
    super(id, name, 0);
  }

}