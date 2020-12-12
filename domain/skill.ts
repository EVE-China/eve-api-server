import { Type } from "./type";

export class Skill extends Type {

  constructor(
    public id: number,
    public name: string,
    public minLevel: number,
    public level: number
  ) {
    super(id, name, 0);
  }
  
}