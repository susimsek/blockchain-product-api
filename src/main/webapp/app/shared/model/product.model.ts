export interface IProduct {
    id?: number;
    title?: string;
    price?: number;
}

export class Product implements IProduct {
    constructor(public id?: number, public title?: string, public price?: number) {}
}
