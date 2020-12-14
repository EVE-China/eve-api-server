/// <reference types="@vertx/core" />
// @ts-check

import { __dirname, __filename } from '@vertx/core';
import { Type } from './domain/type';

vertx
  .createHttpServer()
  .requestHandler(function (req: any) {
    const type = new Type(123, '测试', 0);
    req.response()
      .putHeader("content-type", "text/plain")
      .end(JSON.stringify(type));
  }).listen(3000);

vertx.deployVerticle('./dist/scheduled-task.js');
console.log('Server started on port 3000');

